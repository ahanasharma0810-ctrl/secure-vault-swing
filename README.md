# Secure Vault

A desktop password manager built with **Java Swing**, designed using
object-oriented programming principles (encapsulation, abstraction,
inheritance and polymorphism).

## Planned features
- Register and log in with a master password
- Add, view, edit, delete and search saved credentials
- Strong password generator
- AES-256 encryption of everything stored on disk
- Copy to clipboard with automatic clearing
- Auto-lock when idle

## Project structure
```
src/main/java/com/securevault/
 ├── config/      application settings
 ├── model/       data classes (User, Credential)
 ├── security/    hashing, encryption, password generation
 ├── storage/     saving and loading the vault
 ├── service/     business logic
 ├── ui/          Swing windows and dialogs
 ├── exception/   custom exceptions
 ├── util/        helper classes
 └── Main.java    entry point
```

## Requirements
- Java 11 or newer
- Maven (optional)
