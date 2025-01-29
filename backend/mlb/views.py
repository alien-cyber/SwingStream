from django.http import FileResponse, HttpResponse, HttpResponseNotFound
from django.shortcuts import get_object_or_404
from .models import Video
import os
import re


def stream_video(request, video_id):
   
    
    video = get_object_or_404(Video, id=video_id)
    file_path = video.file.path  # Adjust based on your `Video` model

    # Ensure the file exists
    if not os.path.exists(file_path):
        return HttpResponseNotFound("Video not found")

    # Get file size
    file_size = os.path.getsize(file_path)

    # Handle the "Range" header for byte-range requests
    range_header = request.headers.get("Range")
    if range_header:
        # Example: "bytes=2000-"
        range_match = re.match(r"bytes=(\d+)-(\d+)?", range_header)
        if range_match:
            start = int(range_match.group(1))
            end = int(range_match.group(2)) if range_match.group(2) else file_size - 1
            end = min(end, file_size - 1)  # Ensure end does not exceed file size
            length = end - start + 1

            # Open the file and return the requested byte range
            with open(file_path, "rb") as f:
                f.seek(start)
                content = f.read(length)

            response = HttpResponse(content, content_type="video/mp4")
            response["Content-Length"] = str(length)
            response["Content-Range"] = f"bytes {start}-{end}/{file_size}"
            response.status_code = 206  # Partial content
            return response

    # If no "Range" header, serve the entire file
    response = FileResponse(open(file_path, "rb"), content_type="video/mp4")
    response["Content-Length"] = str(file_size)
    return response
