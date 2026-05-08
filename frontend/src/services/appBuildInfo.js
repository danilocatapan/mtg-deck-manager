export const FRONTEND_INFO = {
  name: import.meta.env.VITE_APP_NAME || 'MTG Deck Manager',
  version: import.meta.env.VITE_APP_VERSION || '0.0.0-local',
  commit: shortCommit(import.meta.env.VITE_COMMIT_SHA || 'local'),
  branch: import.meta.env.VITE_BRANCH || 'local',
  buildTime: import.meta.env.VITE_BUILD_TIME || 'local',
  environment: import.meta.env.VITE_APP_ENVIRONMENT || import.meta.env.MODE || 'local',
}

function shortCommit(commit) {
  return commit && commit !== 'local' ? commit.slice(0, 7) : commit
}
