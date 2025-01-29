from django.contrib import admin
from .models import Video

@admin.register(Video)
class VideoAdmin(admin.ModelAdmin):
    list_display = ('id', 'title', 'file')  
    search_fields = ('title',)  

    def save_model(self, request, obj, form, change):
        print(f"Saving video: {obj.title}, File: {obj.file}")  # Debugging log
        super().save_model(request, obj, form, change)