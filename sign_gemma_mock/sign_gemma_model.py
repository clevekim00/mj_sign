import os
import keras
import keras_nlp
from logger_config import logger

# Set Keras backend to JAX (as used in the notebook)
os.environ["KERAS_BACKEND"] = "jax"
os.environ["XLA_PYTHON_CLIENT_MEM_FRACTION"] = "1.00"

class SignGemmaEngine:
    _instance = None

    def __new__(cls, *args, **kwargs):
        if not cls._instance:
            cls._instance = super(SignGemmaEngine, cls).__new__(cls)
        return cls._instance

    def __init__(self, model_id="gemma2_instruct_2b_en", lora_weights_path=None):
        if hasattr(self, 'initialized') and self.initialized:
            return
        
        self.model_id = model_id
        self.lora_weights_path = lora_weights_path
        self.token_limit = 256
        self.gemma = None
        self.initialized = False

    def load_model(self):
        try:
            logger.info(f"Loading Base Gemma Model: {self.model_id}...")
            # Load causal LM from preset
            self.gemma = keras_nlp.models.GemmaCausalLM.from_preset(self.model_id)
            
            # Enable LoRA (must match the rank used in training)
            # Rank 4 was used in the notebook
            self.gemma.backbone.enable_lora(rank=4)
            self.gemma.preprocessor.sequence_length = self.token_limit

            if self.lora_weights_path and os.path.exists(self.lora_weights_path):
                logger.info(f"Loading LoRA weights from: {self.lora_weights_path}")
                self.gemma.backbone.load_lora_weights(self.lora_weights_path)
            else:
                logger.warning("LoRA weights path not found or invalid. Running with base model only.")

            self.initialized = True
            logger.info("SignGemma Model loaded successfully.")
        except Exception as e:
            logger.error(f"Failed to load SignGemma model: {str(e)}")
            raise e

    def generate(self, prompt: str) -> str:
        if not self.initialized:
            self.load_model()

        input_str = f"<start_of_turn>user\n{prompt}<end_of_turn>\n<start_of_turn>model\n"
        try:
            output = self.gemma.generate(input_str, max_length=self.token_limit)
            # Remove the prompt parts from the output if necessary
            # Simple cleanup for Gemma format
            result = output.split("<start_of_turn>model\n")[-1].replace("<end_of_turn>", "").strip()
            return result
        except Exception as e:
            logger.error(f"Generation error: {str(e)}")
            return f"Error during generation: {str(e)}"

# Global instance for the FastAPI app
engine = SignGemmaEngine()
