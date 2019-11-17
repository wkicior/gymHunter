# gymHunter
A web crawler for hunting free places on Gymsteer platform


# Install
## Prerequisites
Webhook Notification on IFTT (https://github.com/hossman/ifttt-trigger)

event name: ```gymhunter```

Sample message: ```Training {{Value1}} has slots available in club: {{Value2}} ({{OccurredAt}})```

## Required environment variables
GYMHUNTER_PASSWORD - password for basic auth requests
IFTT_KEY = private key for IFTT Webhooks notifications
GYMHUNTER_STORE_DIR - directory for journal and snapshots storage
 
##run locally
```$ ./sbt run```


## Build docker image
```./sbt docker:publishLocal```


### Run docker locally

```docker-compose up```

#Prod setup
###create machine (sample):
```docker-machine create --driver digitalocean --digitalocean-access-token=[TOKEN] --digitalocean-size s-1vcpu-1gb --digitalocean-region fra1 --digitalocean-image fedora-30-x64 gymhunter-droplet```

###show machine env and switch to machine:
```docker-machine env gymhunter```
```eval $(docker-machine env gymhunter-droplet)```

###Build image and publish to machine
```./sbt docker:publishLocal```

###deploy:
```docker-compose -f docker-compose-prod.yml up -d```

## Create TrainingHuntingSubscription
```curl -X POST \
  http://localhost:8080/api/training-hunting-subscriptions \
  -H 'Content-Type: application/json' \
  -d '{
	"externalSystemId": 699316,
	"clubId": 8,
	"huntingDeadline": "2019-11-17T22:00:00+0200"
   }'
```

## Get all subscriptions
```curl http://localhost:8080/api/training-hunting-subscriptions```
