
import aiohttp 
import google.generativeai as genai 
import typing_extensions as typing
import json
import vertexai
from vertexai.generative_models import GenerativeModel



vertexai.init(project="gemini-449109", location="us-central1")

class Prediction(typing.TypedDict):
    description: str
    next_event: str
    


# AIzaSyCPktlmtvX1xJBZagWmNgQqhYeAQy8FIUM
async def process_data(data, timestamp, prevtime):
    """
    Process the fetched data and determine what to send.
    Returns None if no update is needed; otherwise, returns the processed message.
    """
    
    curtime = data["metaData"]["timeStamp"]

    if curtime != prevtime:
        prevtime = curtime
    timestamp = increment_timestamp(timestamp)
    gameevent=data["metaData"]["gameEvents"]
    events_wanted = ["ball","at_bat_start", "foul", "strike","swinging_strike","called_strike","blocked_ball","wild_pitch","foul_strike"]
    for i in events_wanted:
        if i in gameevent:
            count=data['liveData']['plays']['currentPlay']['count']
            matchup=data['liveData']['plays']['currentPlay']['matchup']
            batter=matchup['batter']
            pitcher=matchup['pitcher']
            result=data['liveData']['plays']['currentPlay']["result"]
            istopinnings=data['liveData']['plays']['currentPlay']["about"]["isTopInning"]
            if istopinnings:
                batting="Away team is batting"
            else:
                batting="Home team is batting"


             
            async with aiohttp.ClientSession() as session:
                #  const fullUrl = `https://statsapi.mlb.com${link}?hydrate=stats(group=[hitting],type=[byDateRange],startDate=01/28/2018,endDate=12/20/2018,season=2018)`;
                fullurl='https://statsapi.mlb.com/' + batter["link"]+  '?hydrate=stats(group=[hitting],type=[byDateRange],startDate=01/28/2018,endDate=12/20/2018,season=2018)'
                async with session.get(fullurl) as response:
                    if response.status == 200:
                        batter_data = await response.json()
                fullurl_pitcher='https://statsapi.mlb.com/' + pitcher["link"]+  '?hydrate=stats(group=[pitching],type=[byDateRange],startDate=01/28/2018,endDate=12/20/2018,season=2018)'
                
            async with aiohttp.ClientSession() as session:
                fullurl_pitcher='https://statsapi.mlb.com/' + pitcher["link"]+  '?hydrate=stats(group=[pitching],type=[byDateRange],startDate=01/28/2018,endDate=12/20/2018,season=2018)'

                async with session.get(fullurl_pitcher) as response:
                    if response.status == 200:
                        pitcher_data = await response.json()
            
            try:
                batter_stats=batter_data['people'][0]['stats'][0]['splits'][0]['stat']
                some_batter_stats = dict(list(batter_stats.items())[:10])
                batter_name=batter_data['people'][0]['fullName']
            except KeyError:
                batter_stats="NO DATA "
                batter_name=batter_data['people'][0]['fullName']
            try:
                pitcher_stats=pitcher_data['people'][0]['stats'][0]['splits'][0]['stat']
                some_pitcher_stats = dict(list(pitcher_stats.items())[:10])

                pitcher_name=pitcher_data['people'][0]['fullName']
            except KeyError:
                pitcher_stats="NO DATA "
                pitcher_name=pitcher_data['people'][0]['fullName']
            received={ "next_event": "not",  "description": "[not Name] hits single, starts rally"}
            

            
            prompt_1=f"""Imagine you're a baseball fan chatting with another fan at a game. Based on the current situation, predict what might happen next in the game. Use the player's stats, game count, and a natural, conversational tone.
Here’s the scenario:
Pitcher: {pitcher_name} - {some_pitcher_stats}
Batter: {batter_name}  - {some_batter_stats}
Total outs by pitcher in this match:{count["outs"]}
pitcher vs current batter stats:[balls:{count["balls"]},strikes:{count["strikes"]}]

awayTeamScore:{result["awayScore"]}
homeTeamScore:{result["homeScore"]},
{batting}"""
            prompt_2="""Make your prediction sound like you're analyzing and speculating in real time. For example:
 { \"next_event\": \"Single\", \"description\": \"[Batter Name] hits single, starts rally\" }

Give the answer in json form ,it should contain next_event which is form this list {
  \"hitting_events\": [
    \"Single\",    
    \"Double\",     
    \"Triple\",    
    \"Home Run\",    
    \"Groundout\",   
    \"Flyout\",    
         
    \"Lineout\",    
    \"Strikeout\",  
 
    \"Hit by Pitch\", 
       ],
  \"pitching_events\": [
    \"Strike\",    
    \"Ball\",      
    
    \"Strikeout\"  
  ]} and description in 5-6 words"""
            prompt=prompt_1+prompt_2

            model = GenerativeModel("gemini-1.5-flash-002")
            model_result = model.generate_content(
                           prompt
                            )
            json_string=model_result.text
            if "{" in json_string and "}" in json_string:
                json_string = json_string[json_string.index("{"):json_string.rindex("}") + 1]


            if json_string.strip():
                parsed_data = json.loads(json_string)
                next_event = parsed_data['next_event']
                description = parsed_data['description']

            else:
                return "Hello bro","what will happen next",timestamp,prevtime   
            
            
            return next_event,description, timestamp, prevtime
            
         

     # {  "next_event": "Single",  "description": "[Batter Name] hits single, starts rally" }
     # Don't waste money on api
        
                          
   
    
    
    # received={ "next_event": "Single",  "description": "[Batter Name] hits single, starts rally"}
 
    return None,None, timestamp, prevtime
    # return f"{received['next_event']}",f"{received['description']}", timestamp, prevtime


def increment_timestamp(timestamp):
    """
    Increments the seconds in a timestamp by 10, handling overflow for seconds, minutes, hours, days, months, and years.

    Args:
        timestamp (str): Timestamp in the format "YYYYMMDD_HHMMSS".

    Returns:
        str: Updated timestamp.
    """
    import datetime

    # Parse the input timestamp
    date_part, time_part = timestamp.split("_")
    year = int(date_part[:4])
    month = int(date_part[4:6])
    day = int(date_part[6:])
    hour = int(time_part[:2])
    minute = int(time_part[2:4])
    second = int(time_part[4:])

    # Increment the seconds
    second += 10

    # Handle overflow using datetime
    try:
        dt = datetime.datetime(year, month, day, hour, minute, second)
    except ValueError:
        # If seconds overflow, datetime will handle it
        dt = datetime.datetime(year, month, day, hour, minute, 0) + datetime.timedelta(seconds=second)

    # Convert the updated datetime back to the desired format
    return dt.strftime("%Y%m%d_%H%M%S")


   

