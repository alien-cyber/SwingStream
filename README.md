# SwingStream

## Description
A streaming app for baseball fans along with some extra Ai features(see images)

[Watch the Demo ](https://www.youtube.com/watch?v=r6EG5Z9dVsc&ab_channel=Nanthakumar)


![Alt text](images/screenshot.jpeg)




## Features
- Multiple languages
- AI-Fan interaction about next event
- Content-based filtering and telecasting Homerun videos

## Installation
```sh

git clone https://github.com/alien-cyber/SwingStream.git

cd backend

pip install -r requirements.txt

python manage.py makemigrations

python manage.py migrate


```

## Make sure to login with gcloud auth and replace ID with your project ID in cloud 
```sh

daphne -b 0.0.0.0 -p 8080 backend.asgi:application

```




## License
This project is licensed under the Apache License Version 2.0.


