import asyncio
import json
import aiohttp  # For making asynchronous HTTP requests
from channels.generic.websocket import AsyncWebsocketConsumer
from . import data_processor
import random
from google.cloud import translate


PROJECT_ID = "gemini-449109"

PARENT = f"projects/{PROJECT_ID}"



class UpdateConsumer(AsyncWebsocketConsumer):
    global langcode
    async def connect(self):

        headers = dict((k.decode(), v.decode()) for k, v in self.scope["headers"])
        
        self.langcode = headers.get("lang", "en")
        self.group_name = "updates_group"
        await self.channel_layer.group_add(self.group_name, self.channel_name)
        await self.accept()

        # Start the periodic task
        self.keep_running = True
        asyncio.create_task(self.fetch_and_process_data())

    async def disconnect(self, close_code):
        # Stop the periodic task
        self.keep_running = False
        await self.channel_layer.group_discard(self.group_name, self.channel_name)

    async def send_update(self, event):
        message = event['message']
        await self.send(text_data=json.dumps({"message": message}))

    async def fetch_and_process_data(self):
        timestamp="20180329_201229"
        prevtime="20180329_201229"
        while self.keep_running:
            try:
                print("started")
                # Simulate fetching data from an API
                async with aiohttp.ClientSession() as session:
                    async with session.get(f"https://statsapi.mlb.com/api/v1.1/game/529416/feed/live?timecode={timestamp}") as response:
                        if response.status == 200:
                            data = await response.json()
                            
                            # Process the fetched data
                            (next_event,description,timestamp, prevtime) =await  data_processor.process_data(data,timestamp,prevtime)
                            
                            # Send update if needed
                            if next_event:
                                events = ["strike", "strikeout", "homerun", "walk", "single", "double", "triple", "hit by pitch", "ground out", "fly out"]
                                selected_events = random.sample(events, 3)
                                next_event=translate_text(next_event,self.langcode)
                                description=translate_text(description, self.langcode)
                                selected_events = [translate_text(event, self.langcode) for event in selected_events]




                             
                                await self.channel_layer.group_send(
                                  self.group_name,
                                         {
                                        "type": "send_update",
                                           "message": {
                                      "next_event": next_event,
                                     "description": description,
                                 "selected_events": selected_events
                                                  }
                                              }
                                           )
                                
                                await asyncio.sleep(60)
                            
            except Exception as e:
                print(f"Error fetching or processing data: {e}")

            # Wait for 1 minute before the next fetch
            await asyncio.sleep(10)




def translate_text(text: str, target_language_code: str) -> translate.Translation:
    client = translate.TranslationServiceClient()

    response = client.translate_text(
        parent=PARENT,
        contents=[text],
        target_language_code=target_language_code,
    )

    return response.translations[0].translated_text


