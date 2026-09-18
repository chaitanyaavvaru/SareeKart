import React from 'react';

/**
 * Top-level ErrorBoundary component catching unhandled React render exceptions.
 * Prevents white-screen crashes and renders an accessible luxury fallback UI.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    // Log exception to console or telemetry service
    console.error('Unhandled React exception captured by ErrorBoundary:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  handleHome = () => {
    this.setState({ hasError: false, error: null });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#FAF8F5] flex items-center justify-center p-6 text-center font-sans">
          <div className="max-w-md w-full bg-white rounded-3xl p-8 sm:p-10 border border-[#E6DFD3] shadow-lg space-y-6">
            <div className="w-16 h-16 rounded-2xl bg-[#2B0F1E]/5 text-[#C89B3C] flex items-center justify-center mx-auto">
              <svg
                className="w-8 h-8 stroke-current"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth="1.75"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 7.5h.008v.008H12v-.008z"
                />
              </svg>
            </div>

            <div className="space-y-2">
              <span className="text-[11px] font-bold uppercase tracking-widest text-[#C89B3C]">
                SareeKart Atelier Notice
              </span>
              <h1 className="font-serif text-2xl font-bold text-[#2B0F1E]">
                An Unexpected Exception Occurred
              </h1>
              <p className="text-xs sm:text-sm text-[#22181C]/70 leading-relaxed">
                The atelier encountered a momentary disruption. We have safely preserved your session. Please reload or return to our handloom showcase.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              <button
                type="button"
                onClick={this.handleReset}
                className="flex-1 py-3 px-5 rounded-full text-xs font-bold uppercase tracking-wider bg-[#2B0F1E] text-white hover:bg-[#3D142A] transition-colors cursor-pointer shadow-xs"
              >
                Reload Atelier
              </button>
              <button
                type="button"
                onClick={this.handleHome}
                className="flex-1 py-3 px-5 rounded-full text-xs font-bold uppercase tracking-wider bg-white text-[#2B0F1E] border border-[#E6DFD3] hover:border-[#C89B3C] transition-colors cursor-pointer"
              >
                Return to Showcase
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
