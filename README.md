# Buzz LEAPyear team Project

## Running & Testing

### Whole Project
`docker-compose up -d`

Starts 3 containers: db, app, and web

#### Connect to db:

`docker-compose exec -it db psql -U postgres`

#### Test API endpoints :

`curl localhost:6767/api/v1/<endpoint>`

#### Webserver:

Visit `localhost:<port>` in a browser

You may need to forward the port in vscode first

### Frontend
```
cd frontend
npm run start
```
then visit `localhost:4200`

### Backend
```
cd backend
mvn -B clean package
java -jar target/trading_api-0.0.1-SNAPSHOT.jar.original
```
then `curl localhost:6767/api/v1/<endpoint>`

---



## Members
- Ari Lacanienta
- Billy Nguyen
- Madison Pham
- Josetta Reyes
- William Tran

## Branching Strategy
Trunk-based. We have a small team, and want to have short-lived feature branches that all get pulled into the main trunk branch.
