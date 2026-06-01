import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecr from "aws-cdk-lib/aws-ecr";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as iam from "aws-cdk-lib/aws-iam";
import * as s3 from "aws-cdk-lib/aws-s3";
import * as cloudfront from "aws-cdk-lib/aws-cloudfront";
import * as origins from "aws-cdk-lib/aws-cloudfront-origins";
import * as cognito from "aws-cdk-lib/aws-cognito";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as ssm from "aws-cdk-lib/aws-ssm";
import * as acm from "aws-cdk-lib/aws-certificatemanager";

export class ReporteCiudadanoAdminStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // ── Context values ────────────────────────────────────────────────────────
    const acmCertArn: string = this.node.tryGetContext("acmCertArn") as string;
    const appDomain: string = this.node.tryGetContext("appDomain") as string;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. ECR — backend container registry
    // ─────────────────────────────────────────────────────────────────────────
    const ecrRepository = new ecr.Repository(this, "BackendRepository", {
      repositoryName: "reporte-ciudadano-admin-backend",
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      imageScanOnPush: true,
      lifecycleRules: [
        {
          description: "Keep last 10 images",
          maxImageCount: 10,
          tagStatus: ecr.TagStatus.ANY,
        },
      ],
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 2. VPC — 2 AZs, public + private subnets, 1 NAT gateway
    // ─────────────────────────────────────────────────────────────────────────
    const vpc = new ec2.Vpc(this, "Vpc", {
      vpcName: "reporte-ciudadano-admin-vpc",
      maxAzs: 2,
      natGateways: 1,
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: "public",
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          cidrMask: 24,
          name: "private",
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
      ],
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 3. ECS Fargate cluster
    // ─────────────────────────────────────────────────────────────────────────
    const cluster = new ecs.Cluster(this, "EcsCluster", {
      clusterName: "reporte-ciudadano-admin",
      vpc,
      containerInsights: true,
    });

    // ── Task IAM role ─────────────────────────────────────────────────────────
    const taskRole = new iam.Role(this, "EcsTaskRole", {
      roleName: "reporte-ciudadano-admin-task-role",
      assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
      description: "ECS task role for ReporteCiudadanoAdmin backend",
    });

    // DynamoDB permissions
    taskRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "DynamoDBReports",
        effect: iam.Effect.ALLOW,
        actions: [
          "dynamodb:Scan",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DescribeTable",
        ],
        resources: [
          "arn:aws:dynamodb:us-east-1:literal:<AWS_ACCOUNT_ID>:table/reporte-ciudadano-reports",
        ],
      })
    );

    // S3 permissions (photos bucket — read-only)
    taskRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "S3PhotosRead",
        effect: iam.Effect.ALLOW,
        actions: ["s3:GetObject", "s3:HeadObject", "s3:ListBucket"],
        resources: [
          "arn:aws:s3:::reporte-ciudadano-photos",
          "arn:aws:s3:::reporte-ciudadano-photos/*",
        ],
      })
    );

    // ── Task execution role (ECR pull + CloudWatch logs) ──────────────────────
    const taskExecutionRole = new iam.Role(this, "EcsTaskExecutionRole", {
      roleName: "reporte-ciudadano-admin-task-execution-role",
      assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName(
          "service-role/AmazonECSTaskExecutionRolePolicy"
        ),
      ],
    });

    // Allow execution role to read SSM parameters for env vars
    taskExecutionRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "SSMCognitoParams",
        effect: iam.Effect.ALLOW,
        actions: ["ssm:GetParameters", "ssm:GetParameter"],
        resources: [
          `arn:aws:ssm:us-east-1:literal:<AWS_ACCOUNT_ID>:parameter/reporte-ciudadano-admin/cognito-user-pool-id`,
          `arn:aws:ssm:us-east-1:literal:<AWS_ACCOUNT_ID>:parameter/reporte-ciudadano-admin/cognito-client-id`,
        ],
      })
    );

    // ── SSM Parameter Store references ───────────────────────────────────────
    const cognitoUserPoolIdParam = ssm.StringParameter.fromStringParameterName(
      this,
      "CognitoUserPoolIdParam",
      "/reporte-ciudadano-admin/cognito-user-pool-id"
    );

    const cognitoClientIdParam = ssm.StringParameter.fromStringParameterName(
      this,
      "CognitoClientIdParam",
      "/reporte-ciudadano-admin/cognito-client-id"
    );

    // ── Task definition ───────────────────────────────────────────────────────
    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      "BackendTaskDef",
      {
        family: "reporte-ciudadano-admin-backend",
        cpu: 512,
        memoryLimitMiB: 1024,
        taskRole,
        executionRole: taskExecutionRole,
      }
    );

    const container = taskDefinition.addContainer("BackendContainer", {
      containerName: "backend",
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, "latest"),
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: "reporte-ciudadano-admin-backend",
        logRetention: cdk.aws_logs.RetentionDays.TWO_WEEKS,
      }),
      environment: {},
      secrets: {
        COGNITO_USER_POOL_ID: ecs.Secret.fromSsmParameter(
          cognitoUserPoolIdParam
        ),
        COGNITO_CLIENT_ID: ecs.Secret.fromSsmParameter(cognitoClientIdParam),
      },
      portMappings: [
        {
          containerPort: 8080,
          protocol: ecs.Protocol.TCP,
        },
      ],
      healthCheck: {
        command: [
          "CMD-SHELL",
          "curl -f http://localhost:8080/health || exit 1",
        ],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(60),
      },
    });

    // ── Security groups ───────────────────────────────────────────────────────
    const albSecurityGroup = new ec2.SecurityGroup(this, "AlbSecurityGroup", {
      vpc,
      securityGroupName: "reporte-ciudadano-admin-alb-sg",
      description: "ALB security group — allow HTTP and HTTPS from anywhere",
      allowAllOutbound: true,
    });
    albSecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(80),
      "Allow HTTP"
    );
    albSecurityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(443),
      "Allow HTTPS"
    );

    const ecsSecurityGroup = new ec2.SecurityGroup(this, "EcsSecurityGroup", {
      vpc,
      securityGroupName: "reporte-ciudadano-admin-ecs-sg",
      description: "ECS task security group — allow traffic from ALB only",
      allowAllOutbound: true,
    });
    ecsSecurityGroup.addIngressRule(
      albSecurityGroup,
      ec2.Port.tcp(8080),
      "Allow from ALB"
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ALB — internet-facing with HTTPS + HTTP redirect
    // ─────────────────────────────────────────────────────────────────────────
    const alb = new elbv2.ApplicationLoadBalancer(this, "Alb", {
      loadBalancerName: "reporte-ciudadano-admin-alb",
      vpc,
      internetFacing: true,
      securityGroup: albSecurityGroup,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
    });

    // HTTP → HTTPS redirect listener
    alb.addListener("HttpListener", {
      port: 80,
      protocol: elbv2.ApplicationProtocol.HTTP,
      defaultAction: elbv2.ListenerAction.redirect({
        protocol: "HTTPS",
        port: "443",
        permanent: true,
      }),
    });

    // HTTPS listener
    const certificate = acm.Certificate.fromCertificateArn(
      this,
      "AlbCertificate",
      acmCertArn ||
        "arn:aws:acm:us-east-1:literal:<AWS_ACCOUNT_ID>:certificate/placeholder-replace-via-context"
    );

    const targetGroup = new elbv2.ApplicationTargetGroup(
      this,
      "BackendTargetGroup",
      {
        targetGroupName: "reporte-ciudadano-admin-tg",
        vpc,
        port: 8080,
        protocol: elbv2.ApplicationProtocol.HTTP,
        targetType: elbv2.TargetType.IP,
        healthCheck: {
          path: "/health",
          interval: cdk.Duration.seconds(30),
          timeout: cdk.Duration.seconds(5),
          healthyThresholdCount: 2,
          unhealthyThresholdCount: 3,
          healthyHttpCodes: "200",
        },
        deregistrationDelay: cdk.Duration.seconds(30),
      }
    );

    alb.addListener("HttpsListener", {
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS,
      certificates: [certificate],
      defaultAction: elbv2.ListenerAction.forward([targetGroup]),
    });

    // ── ECS Fargate service ───────────────────────────────────────────────────
    const ecsService = new ecs.FargateService(this, "BackendService", {
      serviceName: "reporte-ciudadano-admin-backend",
      cluster,
      taskDefinition,
      desiredCount: 1,
      securityGroups: [ecsSecurityGroup],
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      assignPublicIp: false,
      circuitBreaker: { rollback: true },
      enableExecuteCommand: true,
    });

    ecsService.attachToApplicationTargetGroup(targetGroup);

    // ─────────────────────────────────────────────────────────────────────────
    // 5. S3 — frontend static assets (private)
    // ─────────────────────────────────────────────────────────────────────────
    const frontendBucket = new s3.Bucket(this, "FrontendBucket", {
      bucketName: "reporte-ciudadano-admin-frontend",
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      publicReadAccess: false,
      versioned: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      encryption: s3.BucketEncryption.S3_MANAGED,
      enforceSSL: true,
    });

    // ─────────────────────────────────────────────────────────────────────────
    // 6. CloudFront — OAC + SPA routing
    // ─────────────────────────────────────────────────────────────────────────

    // Origin Access Control
    const oac = new cloudfront.CfnOriginAccessControl(this, "FrontendOAC", {
      originAccessControlConfig: {
        name: "reporte-ciudadano-admin-oac",
        description: "OAC for ReporteCiudadanoAdmin frontend S3 bucket",
        originAccessControlOriginType: "s3",
        signingBehavior: "always",
        signingProtocol: "sigv4",
      },
    });

    // CloudFront distribution
    const distribution = new cloudfront.Distribution(
      this,
      "FrontendDistribution",
      {
        comment: "ReporteCiudadanoAdmin frontend SPA",
        defaultBehavior: {
          origin: origins.S3BucketOrigin.withOriginAccessControl(
            frontendBucket,
            {
              originAccessLevels: [
                cloudfront.AccessLevel.READ,
              ],
            }
          ),
          viewerProtocolPolicy:
            cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
          cachePolicy: cloudfront.CachePolicy.CACHING_OPTIMIZED,
          allowedMethods: cloudfront.AllowedMethods.ALLOW_GET_HEAD,
          compress: true,
        },
        defaultRootObject: "index.html",
        errorResponses: [
          {
            httpStatus: 403,
            responseHttpStatus: 200,
            responsePagePath: "/index.html",
            ttl: cdk.Duration.seconds(0),
          },
          {
            httpStatus: 404,
            responseHttpStatus: 200,
            responsePagePath: "/index.html",
            ttl: cdk.Duration.seconds(0),
          },
        ],
        priceClass: cloudfront.PriceClass.PRICE_CLASS_100,
        httpVersion: cloudfront.HttpVersion.HTTP2,
        enableIpv6: true,
      }
    );

    // Override OAC on the S3 origin (CDK's L2 OAI still works but OAC is set via L1 override)
    const cfnDistribution = distribution.node
      .defaultChild as cloudfront.CfnDistribution;
    cfnDistribution.addOverride(
      "Properties.DistributionConfig.Origins.0.OriginAccessControlId",
      oac.attrId
    );
    // Remove legacy OAI if present
    cfnDistribution.addOverride(
      "Properties.DistributionConfig.Origins.0.S3OriginConfig.OriginAccessIdentity",
      ""
    );

    // Grant CloudFront OAC read access to the bucket via bucket policy
    frontendBucket.addToResourcePolicy(
      new iam.PolicyStatement({
        sid: "AllowCloudFrontOAC",
        effect: iam.Effect.ALLOW,
        principals: [new iam.ServicePrincipal("cloudfront.amazonaws.com")],
        actions: ["s3:GetObject"],
        resources: [`${frontendBucket.bucketArn}/*`],
        conditions: {
          StringEquals: {
            "AWS:SourceArn": `arn:aws:cloudfront::literal:<AWS_ACCOUNT_ID>:distribution/${distribution.distributionId}`,
          },
        },
      })
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Cognito User Pool
    // ─────────────────────────────────────────────────────────────────────────
    const userPool = new cognito.UserPool(this, "UserPool", {
      userPoolName: "reporte-ciudadano-admin-pool",
      selfSignUpEnabled: false,
      signInAliases: {
        email: true,
        username: false,
      },
      autoVerify: { email: true },
      passwordPolicy: {
        minLength: 8,
        requireUppercase: true,
        requireLowercase: true,
        requireDigits: true,
        requireSymbols: true,
      },
      accountRecovery: cognito.AccountRecovery.EMAIL_ONLY,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      standardAttributes: {
        email: { required: true, mutable: true },
      },
      mfa: cognito.Mfa.OPTIONAL,
      mfaSecondFactor: {
        sms: false,
        otp: true,
      },
    });

    // Build callback and logout URLs conditionally
    const callbackUrls = appDomain
      ? [
          `https://${appDomain}`,
          `https://${appDomain}/callback`,
          "http://localhost:3000",
          "http://localhost:3000/callback",
        ]
      : ["http://localhost:3000", "http://localhost:3000/callback"];

    const logoutUrls = appDomain
      ? [`https://${appDomain}`, "http://localhost:3000"]
      : ["http://localhost:3000"];

    const userPoolClient = new cognito.UserPoolClient(
      this,
      "UserPoolWebClient",
      {
        userPool,
        userPoolClientName: "reporte-ciudadano-admin-web",
        generateSecret: false,
        authFlows: {
          userPassword: true,
          userSrp: false,
          custom: false,
        },
        oAuth: {
          flows: {
            authorizationCodeGrant: true,
            implicitCodeGrant: false,
          },
          scopes: [
            cognito.OAuthScope.EMAIL,
            cognito.OAuthScope.OPENID,
            cognito.OAuthScope.PROFILE,
          ],
          callbackUrls,
          logoutUrls,
        },
        preventUserExistenceErrors: true,
        accessTokenValidity: cdk.Duration.hours(1),
        idTokenValidity: cdk.Duration.hours(1),
        refreshTokenValidity: cdk.Duration.days(30),
        enableTokenRevocation: true,
        supportedIdentityProviders: [
          cognito.UserPoolClientIdentityProvider.COGNITO,
        ],
      }
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 8. GitHub Actions OIDC IAM role
    // ─────────────────────────────────────────────────────────────────────────

    // Reference the existing GitHub OIDC provider (bootstrap it once per account)
    const githubOidcProvider = new iam.OpenIdConnectProvider(
      this,
      "GithubOidcProvider",
      {
        url: "https://token.actions.githubusercontent.com",
        clientIds: ["sts.amazonaws.com"],
        thumbprints: ["6938fd4d98bab03faadb97b34396831e3780aea1"],
      }
    );

    const githubDeployRole = new iam.Role(this, "GithubDeployRole", {
      roleName: "reporte-ciudadano-admin-deploy-role",
      description:
        "Role assumed by GitHub Actions via OIDC for deploy workflows",
      assumedBy: new iam.WebIdentityPrincipal(
        githubOidcProvider.openIdConnectProviderArn,
        {
          StringEquals: {
            "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          },
          StringLike: {
            "token.actions.githubusercontent.com:sub": "repo:*",
          },
        }
      ),
      maxSessionDuration: cdk.Duration.hours(1),
    });

    // ECR push permissions
    githubDeployRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "ECRAuth",
        effect: iam.Effect.ALLOW,
        actions: ["ecr:GetAuthorizationToken"],
        resources: ["*"],
      })
    );

    githubDeployRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "ECRPush",
        effect: iam.Effect.ALLOW,
        actions: [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage",
          "ecr:DescribeRepositories",
          "ecr:ListImages",
        ],
        resources: [ecrRepository.repositoryArn],
      })
    );

    // ECS deploy permissions
    githubDeployRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "ECSUpdateService",
        effect: iam.Effect.ALLOW,
        actions: [
          "ecs:UpdateService",
          "ecs:DescribeServices",
          "ecs:RegisterTaskDefinition",
          "ecs:DescribeTaskDefinition",
          "ecs:ListTaskDefinitions",
        ],
        resources: [
          ecsService.serviceArn,
          `arn:aws:ecs:us-east-1:literal:<AWS_ACCOUNT_ID>:task-definition/reporte-ciudadano-admin-backend:*`,
        ],
      })
    );

    // Allow passing the task roles to ECS (required for register-task-definition)
    githubDeployRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "PassTaskRoles",
        effect: iam.Effect.ALLOW,
        actions: ["iam:PassRole"],
        resources: [taskRole.roleArn, taskExecutionRole.roleArn],
      })
    );

    // S3 sync permissions
    githubDeployRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "S3FrontendSync",
        effect: iam.Effect.ALLOW,
        actions: [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket",
          "s3:GetBucketLocation",
        ],
        resources: [
          frontendBucket.bucketArn,
          `${frontendBucket.bucketArn}/*`,
        ],
      })
    );

    // CloudFront invalidation permissions
    githubDeployRole.addToPolicy(
      new iam.PolicyStatement({
        sid: "CloudFrontInvalidate",
        effect: iam.Effect.ALLOW,
        actions: [
          "cloudfront:CreateInvalidation",
          "cloudfront:GetInvalidation",
          "cloudfront:ListInvalidations",
        ],
        resources: [
          `arn:aws:cloudfront::literal:<AWS_ACCOUNT_ID>:distribution/${distribution.distributionId}`,
        ],
      })
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 9. CfnOutputs
    // ─────────────────────────────────────────────────────────────────────────
    new cdk.CfnOutput(this, "AlbDnsName", {
      exportName: "ReporteCiudadanoAdmin-AlbDnsName",
      value: alb.loadBalancerDnsName,
      description: "ALB DNS name — set your backend API DNS CNAME to this",
    });

    new cdk.CfnOutput(this, "CloudFrontDomain", {
      exportName: "ReporteCiudadanoAdmin-CloudFrontDomain",
      value: distribution.domainName,
      description:
        "CloudFront distribution domain — set your app domain CNAME to this",
    });

    new cdk.CfnOutput(this, "CognitoUserPoolId", {
      exportName: "ReporteCiudadanoAdmin-CognitoUserPoolId",
      value: userPool.userPoolId,
      description: "Cognito User Pool ID",
    });

    new cdk.CfnOutput(this, "CognitoClientId", {
      exportName: "ReporteCiudadanoAdmin-CognitoClientId",
      value: userPoolClient.userPoolClientId,
      description: "Cognito App Client ID (no secret)",
    });

    new cdk.CfnOutput(this, "EcrRepositoryUri", {
      exportName: "ReporteCiudadanoAdmin-EcrRepositoryUri",
      value: ecrRepository.repositoryUri,
      description:
        "ECR repository URI — docker push target for backend images",
    });

    new cdk.CfnOutput(this, "GithubDeployRoleArn", {
      exportName: "ReporteCiudadanoAdmin-GithubDeployRoleArn",
      value: githubDeployRole.roleArn,
      description:
        "IAM role ARN assumed by GitHub Actions via OIDC — set as AWS_ROLE_ARN in GitHub Actions secrets",
    });

    new cdk.CfnOutput(this, "EcsClusterName", {
      exportName: "ReporteCiudadanoAdmin-EcsClusterName",
      value: cluster.clusterName,
      description: "ECS cluster name",
    });

    new cdk.CfnOutput(this, "EcsServiceName", {
      exportName: "ReporteCiudadanoAdmin-EcsServiceName",
      value: ecsService.serviceName,
      description: "ECS service name",
    });

    new cdk.CfnOutput(this, "FrontendBucketName", {
      exportName: "ReporteCiudadanoAdmin-FrontendBucketName",
      value: frontendBucket.bucketName,
      description: "S3 bucket for frontend static assets",
    });

    new cdk.CfnOutput(this, "CloudFrontDistributionId", {
      exportName: "ReporteCiudadanoAdmin-CloudFrontDistributionId",
      value: distribution.distributionId,
      description:
        "CloudFront distribution ID — used by cd-frontend.yml to invalidate cache",
    });
  }
}
