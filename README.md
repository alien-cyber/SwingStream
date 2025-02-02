# SwingStream

## Description
A streaming app for baseball fans along with some extra Ai features(see images)

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

## Contributing
1. Fork the repository
2. Create a new branch (`git checkout -b feature-branch`)
3. Commit your changes (`git commit -m 'Add new feature'`)
4. Push to the branch (`git push origin feature-branch`)
5. Create a Pull Request

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact
For any issues or suggestions, contact [your-email@example.com](mailto:your-email@example.com).
