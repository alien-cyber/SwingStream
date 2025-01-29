from django.urls import path
from . import views
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('video/<int:video_id>/', views.stream_video, name='stream_video'),
]
