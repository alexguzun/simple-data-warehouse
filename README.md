# Simple Data Warehouse

## API Documentation
The API documentation can be checked at this link: `localhost:8080/swagger-ui.html`  

## Running locally

Execute:
```
./gradlew clean build
docker-compose build
docker-compose up -d
```

## CSV file data ingestion
Before running any queries, the data must be loaded from a CSV file:
```
curl -X POST localhost:8080/ingest -H "Content-Type: application/json" -d '{
   "url" : "<csv_file_url>"
}'
```

### Query Examples
#### Total _Clicks_ for a given _Datasource_ for a given _Date_ range
```
curl -X POST localhost:8080/query/ -H "Content-Type: application/json" -d  '{
    "filter": {
        "Datasource" : { "eq": "Google Ads" },
        "Daily" : {
            "gte" : "01/15/19",
            "lte" : "11/30/19"
        }
    },
    "groupBy" : [
      "Campaign"
    ],
    "aggregate" : {
        "Clicks" : "sum"
    }
}'
```

#### _Impressions_ over time (daily)
```
curl -X POST localhost:8080/query/ -H "Content-Type: application/json" -d  '{
        "groupBy" : [
            "Daily"
        ],
        "aggregate" : {
            "Impressions" : "sum"
        }
    }'
```

#### Group by multiple _dimensions_ and multiple aggregated _metrics_
```
curl -X POST localhost:8080/query/ -H "Content-Type: application/json" -d  '{
        "groupBy" : [
            "Datasource", "Campaign"
        ],
        "aggregate" : {
            "Impressions" : "sum",
            "Clicks": "sum"
        }
}'
```

#### Raw MongoDB aggregation query request
```
curl -X POST localhost:8080/query/raw -H "Content-Type: application/json" -d  '
[
    {
        "$group": {
        "_id": { "Datasource" : "$Datasource", "Campaign": "$Campaign" },
        "Clicks" : {"$sum" : "$Clicks"},
        "Impressions": {"$sum": "$Impressions"}
        }
    },
    {
        "$project" : { "_id": 0, "Datasource" : "$_id.Datasource", "Campaign" : "$_id.Campaign", "ctr": { "$multiply" :[ { "$divide" : [ "$Clicks", "$Impressions" ] }, 100 ]} }
    }
]'
```

## Things to improve/add
- Support for calculated metrics in query DSL
- Run ingestion job in background
- Improve the documentation
- BDD tests