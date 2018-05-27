# sports-computer-api

```
./gradlew bootRun
```

```
curl -v localhost:8080/gameresults -d '[{"teamScores":[{"team":"a","score":1.0},{"team":"b","score":2.0}]},{"teamScores":[{"team":"a","score":3.0},{"team":"b","score":1.0}]}]' -H "Content-type: application/json"
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> POST /gameresults HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.52.1
> Accept: */*
> Content-type: application/json
> Content-Length: 135
>
* upload completely sent off: 135 out of 135 bytes
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: application/json
<
* Curl_http_done: called premature == 0
* Connection #0 to host localhost left intact
[{"team":"a","rank":0.5},{"team":"b","rank":-0.5}]
```

```
curl localhost:8080/mlb/2018/srs | python -m json.tool
[
    {
        "rank": 2.2610597949461058,
        "team": "Houston Astros"
    },
    {
        "rank": 1.5299859374040812,
        "team": "New York Yankees"
    },
    {
        "rank": 1.4790091203278979,
        "team": "Boston Red Sox"
    },
    {
        "rank": 1.2568753736020184,
        "team": "Chicago Cubs"
    },
    {
        "rank": 1.0756657835560286,
        "team": "Atlanta Braves"
    },
    {
        "rank": 0.7922812850299399,
        "team": "Washington Nationals"
    },
    {
        "rank": 0.7588742304898738,
        "team": "Philadelphia Phillies"
    },
    {
        "rank": 0.6242780278818277,
        "team": "Los Angeles Angels"
    },
    {
        "rank": 0.5690142460379527,
        "team": "Milwaukee Brewers"
    },
    {
        "rank": 0.3991776377188232,
        "team": "St. Louis Cardinals"
    },
    {
        "rank": 0.3856676283372216,
        "team": "Pittsburgh Pirates"
    },
    {
        "rank": 0.34356106671942316,
        "team": "Los Angeles Dodgers"
    },
    {
        "rank": 0.2630288778563745,
        "team": "Cleveland Indians"
    },
    {
        "rank": 0.20740832282289764,
        "team": "Arizona Diamondbacks"
    },
    {
        "rank": 0.14137202057593753,
        "team": "Seattle Mariners"
    },
    {
        "rank": -0.05173969333770401,
        "team": "Oakland Athletics"
    },
    {
        "rank": -0.22468020446561518,
        "team": "Tampa Bay Rays"
    },
    {
        "rank": -0.32158872368695673,
        "team": "Toronto Blue Jays"
    },
    {
        "rank": -0.341886481141491,
        "team": "Minnesota Twins"
    },
    {
        "rank": -0.3872162018844725,
        "team": "New York Mets"
    },
    {
        "rank": -0.5247088526835675,
        "team": "Detroit Tigers"
    },
    {
        "rank": -0.5264596423856818,
        "team": "Colorado Rockies"
    },
    {
        "rank": -0.7026861061207338,
        "team": "San Francisco Giants"
    },
    {
        "rank": -0.8780421532457405,
        "team": "San Diego Padres"
    },
    {
        "rank": -0.9822192566080172,
        "team": "Cincinnati Reds"
    },
    {
        "rank": -1.1083843547290653,
        "team": "Texas Rangers"
    },
    {
        "rank": -1.3764834505114827,
        "team": "Baltimore Orioles"
    },
    {
        "rank": -1.4706818153112735,
        "team": "Chicago White Sox"
    },
    {
        "rank": -1.5580896448766375,
        "team": "Kansas City Royals"
    },
    {
        "rank": -1.6804894201837985,
        "team": "Miami Marlins"
    }
]
```
