# Development Prompt: Gemma 2 LLM Integration

This document preserves the original development prompt used to implement the LLM-based sign language translation layer.

---

### [Role Setting]
You are a Kotlin and Spring Boot expert and an AI model serving infrastructure architect. You are in charge of building the backend system for the ongoing 'Sign Language Recognition Service'.

### [Business Goal]
In order to correct text or data extracted from the sign language recognition model and provide natural responses to users, the Gemma 2 (LLM) model must be integrated into the Spring Boot app.

### [Tech Stack & Requirements]
- **Language & Framework**: Kotlin, Spring Boot 3.x
- **AI Integration**: Spring AI (Ollama integration method)
- **Model**: Gemma 2 (Using local Ollama server)

### [Core Tasks]
1. Write the configuration (`application.yml`) using the Spring AI Ollama Starter.
2. Implement `SignTranslationService` that receives sign language keywords (e.g., "I", "rice", "eat") and converts them into natural Korean sentences.
3. Design an interface-based AI model communication structure considering external API expansion.

### [Instructions]
- **Project Setup**: Add the necessary dependencies (Spring AI, Ollama) to `build.gradle.kts`.
- **Configuration**: Write an `application.yml` example including Ollama's default URL (`localhost:11434`) and Gemma model settings.
- **Implementation**:
    - `SignController`: Endpoint receiving sign language data from the client.
    - `GemmaService`: Logic to deliver prompts to Gemma using `ChatClient` and receive responses.
    - **Prompt Engineering**: Include a system prompt in the service logic that induces the grammatic refinement of sign language-specific grammar (word lists) into natural sentences.

### [Output Format]
- Code blocks must be written in Kotlin.
- Add brief explanations for the role of each component.
