# Buzz LEAPyear team Project

## Members
- Ari Lacanienta
- Billy Nguyen
- Madison Pham
- Josetta Reyes
- William Tran

## Branching Strategy
Trunk-based. We have a small team, and want to have short-lived feature branches that all get pulled into the main trunk branch.

## Local Development

### Starting the Platform
```bash
# First time setup (builds images)
docker-compose up --build

# Subsequent runs
docker-compose up
```
### Accessing the Platform
Check your `docker-compose.yml` for the port mappings. Typically:
- **Frontend**: `http://localhost:yourport` (your configured frontend port)
- **Backend API**: `http://localhost:yourport` (your configured backend port)

If ports differ on your machine, check:
```bash
docker-compose ps
```

This shows which ports are mapped. Update `http://localhost:<port>` accordingly.

### Before Committing
- Ensure all tests pass locally
- Add tests for any new features or bug fixes
- Aim for >80% code coverage on critical paths (auth, orders, audit)

### Continuous Integration
Tests run automatically on pull requests to `main`. All tests must pass before merging.