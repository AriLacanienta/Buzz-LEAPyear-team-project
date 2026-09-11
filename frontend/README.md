# Angular Application

A basic Angular project created with Angular 17.

## Project Structure

```
src/
├── app/
│   ├── app.component.ts
│   ├── app.component.html
│   ├── app.component.scss
│   └── app.module.ts
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
├── main.ts
├── index.html
└── styles.scss
```

## Prerequisites

- Node.js (v18.0.0 or higher)
- npm (v9.0.0 or higher)
- Angular CLI (optional, but recommended)

## Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Development Server

To run the development server:

```bash
npm start
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Build

To build the project for production:

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

## Running Tests

Execute the unit tests via [Karma](https://karma-runner.github.io):

```bash
npm test
```

## File Structure Details

- **src/app/**: Main application components and modules
- **src/environments/**: Environment-specific configuration
- **src/main.ts**: Application entry point
- **src/index.html**: Main HTML file
- **src/styles.scss**: Global styles
- **angular.json**: Angular CLI configuration
- **tsconfig.json**: TypeScript configuration

## Key Commands

| Command | Description |
|---------|-------------|
| `npm start` | Runs the development server |
| `npm run build` | Builds the application for production |
| `npm run watch` | Builds the application in watch mode |
| `npm test` | Runs the test suite |
| `npm run lint` | Runs linting |

## Contributing

To extend this Angular application:

1. Create new components using Angular CLI or manually
2. Add routing in a new routing module
3. Implement services for business logic
4. Use HttpClientModule for API calls

## License

MIT
