# gymHunter
A web crawler for hunting free places on Gymsteer platform


# Install
## Prerequisites
Webhook Notification on IFTT (https://github.com/hossman/ifttt-trigger)

event name: ```gymhunter```

Message: ```Training {{Value1}} has slots available in club: {{Value2}} ({{OccurredAt}})```

## Required environment variables
GYMHUNTER_PASSWORD - password for basic auth requests
IFTT_KEY = private key for IFTT Webhooks notifications
 
run
```$ ./sbt run```

# Run
## Create TrainingHuntingSubscription
```curl -X POST \
  http://localhost:8080/api/training-hunting-subscriptions \
  -H 'Content-Type: application/json' \
  -d '{
	"externalSystemId": 699316,
	"clubId": 8,
	"huntingEndTime": "2019-11-17T22:00:00+0200"
   }'
```

## Get all subscriptions
```curl http://localhost:8080/api/training-hunting-subscriptions```
