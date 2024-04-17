# MultiTool API

This repository contains a Spring Boot application called MultiTool API. It provides various functionalities through RESTful endpoints for performing tasks such as inverting phrases, converting temperature units, and fetching random jokes related to DevOps or ICT.

## Technologies Used

- Java
- Spring Boot
- JSON
- RESTful API
- OpenAI API

## Functionality

The MultiTool API application provides the following functionalities:

1. **Invert Phrase**: Accepts a phrase as input and returns the inverted version of the phrase.

    - **Endpoint**: `/invert`
    - **Parameters**:
        - `phrase`: The input phrase to be inverted.
        - `output` (optional): Specifies the output format (`text`, `json`, `xml`, or `html`).
    - **Example**: `/invert?phrase=hello`

2. **Fahrenheit to Celsius Conversion**: Accepts a Fahrenheit temperature as input and returns the equivalent temperature in Celsius.

    - **Endpoint**: `/f2c`
    - **Parameters**:
        - `fahrenheit`: The temperature in Fahrenheit to be converted.
        - `output` (optional): Specifies the output format (`text`, `json`, `xml`, or `html`).
    - **Example**: `/f2c?fahrenheit=98.6`

3. **Celsius to Fahrenheit Conversion**: Accepts a Celsius temperature as input and returns the equivalent temperature in Fahrenheit.

    - **Endpoint**: `/c2f`
    - **Parameters**:
        - `celsius`: The temperature in Celsius to be converted.
        - `output` (optional): Specifies the output format (`text`, `json`, `xml`, or `html`).
    - **Example**: `/c2f?celsius=37`

4. **Random Joke**: Fetches a random joke related to DevOps or ICT using the OpenAI API.

    - **Endpoint**: `/random-joke`
    - **Parameters**:
        - `output` (optional): Specifies the output format (`text`, `json`, `xml`, or `html`).
    - **Example**: `/random-joke`

## Usage

To use the MultiTool API, you can send HTTP requests to the specified endpoints with appropriate parameters. The API supports multiple output formats such as plain text, JSON, XML, and HTML.

## Documentation

Detailed documentation about the API endpoints and their usage is available on the [Swagger UI page](http://localhost:8080/swagger-ui/).

## Deployment

This application can be deployed as a standalone Spring Boot application. It can also be containerized using Docker for easier deployment and scalability.

## Dependencies

- Spring Boot Starter Web
- JSON Processing Library
- OpenAI API Client

## Contributors

- Lloyd
