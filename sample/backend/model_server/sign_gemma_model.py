import os
from logger_config import logger
from profile_registry import SignGemmaProfile

# Set Keras backend to JAX (as used in the notebook)
os.environ["KERAS_BACKEND"] = "jax"
os.environ["XLA_PYTHON_CLIENT_PREALLOCATE"] = "false"

class SignGemmaEngine:
    def __init__(self, profile: SignGemmaProfile):
        self.profile = profile
        self.model_id = profile.model_id
        self.lora_weights_path = profile.lora_weights_path
        self.token_limit = 256
        self.gemma = None
        self.initialized = False

    def load_model(self):
        try:
            import keras_nlp

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
            logger.info("SignGemma profile %s loaded successfully.", self.profile.model_profile)
        except Exception as e:
            logger.error("Failed to load SignGemma profile %s: %s", self.profile.model_profile, str(e))
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

class SignGemmaEngineRegistry:
    def __init__(self):
        self._engines = {}

    def get_engine(self, profile: SignGemmaProfile) -> SignGemmaEngine:
        if profile.model_profile not in self._engines:
            self._engines[profile.model_profile] = SignGemmaEngine(profile)
        return self._engines[profile.model_profile]

    def load_profile(self, profile: SignGemmaProfile) -> None:
        self.get_engine(profile).load_model()

    def generate(self, profile: SignGemmaProfile, prompt: str) -> str:
        return self.get_engine(profile).generate(prompt)

    def is_loaded(self, model_profile: str) -> bool:
        engine = self._engines.get(model_profile)
        return bool(engine and engine.initialized)

    def loaded_profiles(self) -> list[str]:
        return [
            profile
            for profile, engine in self._engines.items()
            if engine.initialized
        ]


engine_registry = SignGemmaEngineRegistry()
