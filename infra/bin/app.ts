#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { ReporteCiudadanoAdminStack } from "../lib/reporte-ciudadano-admin-stack";

const app = new cdk.App();

new ReporteCiudadanoAdminStack(app, "ReporteCiudadanoAdminStack", {
  env: {
    account: "literal:<AWS_ACCOUNT_ID>",
    region: "us-east-1",
  },
  description:
    "ReporteCiudadanoAdmin — ECS Fargate backend, CloudFront+S3 frontend, Cognito auth (FEAT-008)",
});

app.synth();
