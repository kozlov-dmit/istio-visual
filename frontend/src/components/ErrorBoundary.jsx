import React from 'react';

/**
 * Простая реализация Error Boundary для React (class component required).
 * Показывает fallback UI и логирует ошибку в консоль.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, info: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // логируем — сюда можно отправлять на Sentry/Log service
    console.error('ErrorBoundary caught', error, info);
    this.setState({ info });
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 20 }}>
          <h2>Произошла ошибка в интерфейсе</h2>
          <div style={{ whiteSpace: 'pre-wrap', background: '#fff5f5', padding: 12, borderRadius: 6 }}>
            <strong>{String(this.state.error)}</strong>
            <div style={{ marginTop: 8, fontSize: 12, color: '#444' }}>
              {(this.state.info && this.state.info.componentStack) || ''}
            </div>
          </div>
          <div style={{ marginTop: 12 }}>
            Попробуйте перезагрузить страницу. Если ошибка повторяется — пришлите stack trace.
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
