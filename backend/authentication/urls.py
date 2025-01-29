from django.urls import path
from .views import RegisterView, LoginView, UserPreferencesView

urlpatterns = [
    path('register/', RegisterView.as_view(), name='register'),
    path('login/', LoginView.as_view(), name='login'),
    path('preferences/', UserPreferencesView.as_view(), name='preferences'),
]
