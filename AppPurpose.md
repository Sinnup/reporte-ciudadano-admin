# ReporteCiudadanoAdmin — App Purpose

## What This Is

A web-based government administration dashboard for tracking and managing citizen pothole reports submitted through the **ReporteCiudadano** mobile app.

## Who Uses It

**Government officials** — municipal staff responsible for road maintenance and urban infrastructure. They are not technical users; the interface must be simple and focused.

## What It Does

- Displays all incoming citizen reports (title, description, location, photos, submission date)
- Allows officials to change a report's status through a defined lifecycle
- Provides a geographic map view of all reports
- Filters/searches reports by status, date, or location

## Data Source

The mobile app (`ReporteCiudadano`) syncs reports to:
- **DynamoDB** table: `reporte-ciudadano-reports` (us-east-1)
  - Attributes: `id` (PK), `title`, `description`, `latitude`, `longitude`, `status`, `createdAt`
- **S3** bucket: `reporte-ciudadano-photos` (us-east-1)
  - Object key format: `reports/<reportId>/<filename>`

## Report Status Lifecycle

The admin dashboard controls status transitions. The mobile app sets `SENT` on submission; the admin side handles all subsequent state changes.

| Status (admin) | DynamoDB value | Meaning |
|---|---|---|
| Seen | `SEEN` | Official has viewed the report |
| Captured | `PENDING` | Logged into the work system |
| In Plan | `IN_PROGRESS` | Scheduled for repair |
| In Process | `IN_PROGRESS` | Repair actively underway |
| Complete | `RESOLVED` | Pothole fixed and verified |
| Discarded | `DISCARDED` | Duplicate, out-of-scope, or invalid |

## Constraints

- Web only (browser-based)
- Must not require mobile app changes — reads from existing DynamoDB/S3 resources
- Credentials and AWS config must never be committed to source control
