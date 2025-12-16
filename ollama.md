- Creating a custom AI model with Ollama:
  * ollama create custom-mistral-model -f ./ollama/custom-mistral-modelfile
  * ollama run custom-mistral-model
- Steps Explained:
  * Ollama reads the Modelfile
  * Applies it on top of the base model (FROM mistral:7b)
  * Saves a new local model called custom-mistral with your custom behavior.