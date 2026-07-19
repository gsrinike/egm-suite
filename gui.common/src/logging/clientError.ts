export class HttpClientError extends Error {
  constructor(
    message: string,
    readonly url: string,
    readonly status: number,
    readonly statusText: string,
    readonly responseBody: string,
    readonly responseHeaders: Record<string, string>
  ) {
    super(`${message}: ${status}`);
    this.name = 'HttpClientError';
  }

  static async fromResponse(message: string, url: string, response: Response): Promise<HttpClientError> {
    return new HttpClientError(
      message,
      url,
      response.status,
      response.statusText,
      await response.clone().text(),
      Object.fromEntries(response.headers.entries())
    );
  }
}

export function logClientError(context: string, error: unknown, details: Record<string, unknown> = {}) {
  const payload = error instanceof HttpClientError
    ? {
        ...details,
        url: error.url,
        status: error.status,
        statusText: error.statusText,
        responseHeaders: error.responseHeaders,
        responseBody: error.responseBody
      }
    : details;

  console.group(context);
  console.error(error);
  console.error('Context', payload);
  if (error instanceof Error && error.stack) {
    console.error('Stack', error.stack);
  }
  console.trace(`${context} trace`);
  console.groupEnd();
}
