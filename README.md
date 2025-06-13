# Roady - Road Damage Detection App  
  
An Android application that uses AI-powered computer vision to detect and analyze road damage, providing intelligent reporting and location-based insights.  
  
## Features  
  
- **AI-Powered Road Damage Detection**: Uses Roboflow computer vision API to identify potholes, patches, and fatigue cracks  
- **Smart Analysis**: Integrates OpenAI ChatGPT for contextual damage explanations  
- **Location Services**: Google Maps integration with geocoding and reverse geocoding  
- **Adjustable Confidence Threshold**: Real-time filtering of detection results  
- **Interactive Map Interface**: Search locations and view damage reports geographically  
  
## Technology Stack  
  
- **Platform**: Android (Java)  
- **Computer Vision**: Roboflow API (road-damage-fhdff model)  
- **AI Assistant**: OpenAI GPT-3.5-turbo  
- **Maps & Location**: Google Maps API, FusedLocationProviderClient  
- **Networking**: Android Volley  
- **UI**: Material Design components  
  
## Architecture  
  
The app follows a fragment-based architecture with the main functionality centered in `DevicesFragment` [1](#0-0) , which coordinates between:  
  
- Image processing and Roboflow API integration  
- OpenAI ChatGPT for damage explanations    
- Google Maps for location services  
- UI components for user interaction  
  
## Setup & Configuration  
  
### Prerequisites  
  
- Android Studio  
- Android SDK (API level 31+)  
- Google Maps API key  
- Roboflow API key  
- OpenAI API key  
  
### API Keys Configuration  
  
The app requires several API keys configured in the source code:  
  
1. **Google Maps API**: Set in AndroidManifest.xml [2](#0-1)   
2. **Roboflow API**: Configured in DevicesFragment [3](#0-2)   
3. **OpenAI API**: Set in DevicesFragment constants [4](#0-3)   
  
### Permissions  
  
The app requires the following permissions [5](#0-4) :  
  
- Camera access for image capture  
- Internet and network state for API calls  
- Location services for GPS functionality  
- Storage permissions for image handling  
  
## Usage  
  
1. **Capture Image**: Take a photo of road damage using the camera  
2. **AI Analysis**: The app automatically processes the image through Roboflow's computer vision model  
3. **Adjust Confidence**: Use the confidence threshold slider to filter detection results [6](#0-5)   
4. **Get AI Insights**: Receive contextual explanations from ChatGPT about the detected damage  
5. **Location Context**: View damage location on the integrated map with address details  
  
## Damage Detection Classes  
  
The app can detect and classify three types of road damage:  
  
- **Pothole**: Classified as "Major Damage"  
- **Patch**: Classified as "Moderate Damage"    
- **Fatigue Crack**: Classified as "Minimum Damage"  
  
Classification mapping is handled in `getDamageScale()` method [7](#0-6) .  
  
## Development  
  
### Key Components  
  
- `MainActivity`: Navigation orchestrator and entry point  
- `DevicesFragment`: Main functionality hub for damage detection  
- `fragment_devices.xml`: UI layout with map, controls, and results display  
  
### External Integrations  
  
- **Roboflow**: Road damage detection model endpoint  
- **OpenAI**: ChatGPT integration for damage explanations  
- **Google Services**: Maps, geocoding, and location services  
