# Development Setup

This is a standard Jenkins plugin built with Java and Maven.

### Prerequisites

* **Java Development Kit (JDK) 17** or newer.
* **Apache Maven** 3.9.0 or newer.

> [!TIP]
> If you have Nix set up, you can use the Nix flake in the root of the project
> to install all project dependencies automatically. The flake also exposes
> named commands once you're inside the shell

### Building and Running Locally

1.  **Build the plugin:**
    ```bash
    mvn clean package
    ```

2.  **Run a local Jenkins instance for development:**
    ```bash
    mvn hpi:run
    ```
    This will download a sandboxed Jenkins instance, install your plugin, and start it.
    * Access it at `http://localhost:8080/jenkins/`
    
3. **Create a Freestyle Project with the plugin:**
   - From the dashboard, create a `New Item` (which should take you to the page: `http://localhost:8080/jenkins/view/all/newJob`)
   - From there, select `Freestyle Project` calling the new job whatever you want
   - The NowSecure plugin can now be selected from the `Add Build Step` dropdown

### Running Tests

To run the full suite run the command: 

```bash
mvn clean test
```
