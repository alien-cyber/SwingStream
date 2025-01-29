from django.http import JsonResponse
from sklearn.metrics.pairwise import cosine_similarity
import joblib  # For loading the TF-IDF model
import numpy as np
from django.views.decorators.csrf import csrf_exempt
import random
from collections import defaultdict

players = [
    
    "Hank Aaron",
    "Joseph Wilbur Adcock",
    "Tommie Lee Agee",
    "Grover Cleveland Alexander",
    "Cap Anson",
    "Luis Aparicio",
    "Rich Ashburn",
    "Ernie Banks",
    "Hank Bauer",
    "Mark Henry Belanger",
    "Cool Papa Bell",
    "Johnny Bench",
    "Charles Albert Bender",
    "Yogi Berra",
    "Ewell Blackwell",
    "Barry Bonds",
    "Bobby Bonds",
    "Lou Boudreau",
    "Clete Boyer",
    "Lou Brock",
    "Miguel Cabrera",
    "Ken Caminiti",
    "Roy Campanella",
    "Rod Carew",
    "Steve Carlton",
    "Chico Carrasquel",
    "Gary Carter",
    "Oscar Charleston"
]
added_urls = set()
# Load the saved TF-IDF model and metadata
MODEL_PATH = "recommendation/data/tfidf_vectors_with_metadata.pkl"
with open(MODEL_PATH, "rb") as f:
    data = joblib.load(f)

vectors = data["vectors"]  # TF-IDF vectors
vectorizer = data["vectorizer"]  # Fitted TfidfVectorizer
metadata = data["metadata"]  # List of metadata (description and URL)


@csrf_exempt
def homerun_urls(request):
    if request.method == 'POST':
        # Extract the search query from the POST data
        search_description = request.POST.get('query', '')
        top_n = int(request.POST.get('top_n', 3))

        # Add random items to the search query for variety
        random_items = random.sample(players, 6)
        search_description = search_description + ','.join(random_items)  

        # Transform the search query into a TF-IDF vector
        query_vector = vectorizer.transform([search_description])

        # Calculate cosine similarity between the query and all TF-IDF vectors
        similarities = cosine_similarity(query_vector, vectors).flatten()

        # Group metadata by a specific key
        grouped_metadata = defaultdict(list)
        for idx, meta in enumerate(metadata):
            group_key = meta.get("team", "unknown")
            grouped_metadata[group_key].append((idx, similarities[idx]))

        # Select top N diverse matches
        top_matches = []
        
        for group, items in grouped_metadata.items():
            items = sorted(items, key=lambda x: x[1], reverse=True)
            for idx, _ in items:
                if metadata[idx]["url"] not in added_urls:
                    top_matches.append({
                        "description": metadata[idx]["description"],
                        "url": metadata[idx]["url"],
                    })
                    added_urls.add(metadata[idx]["url"])
                if len(top_matches) >= top_n:
                    break
            if len(top_matches) >= top_n:
                break

        # Fill remaining matches if needed
        if len(top_matches) < top_n:
            remaining_indices = np.argsort(similarities)[::-1]  # Descending order
            for idx in remaining_indices:
                if len(top_matches) >= top_n:
                    break
                if metadata[idx]["url"] not in added_urls:
                    top_matches.append({
                        "description": metadata[idx]["description"],
                        "url": metadata[idx]["url"],
                    })
                    added_urls.add(metadata[idx]["url"])

        # Send the top matches as a JSON response
        return JsonResponse({'query': search_description, 'top_matches': top_matches})

    return JsonResponse({'error': 'Invalid request'}, status=400)

