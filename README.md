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