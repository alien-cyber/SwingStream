from django.urls import path
from . import views

urlpatterns = [
    path('homerun-urls/', views.homerun_urls, name='filter_urls'),
]
