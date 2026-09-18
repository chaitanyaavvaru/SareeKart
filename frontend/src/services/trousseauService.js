import api from '../api/axiosConfig';

/**
 * Trousseau Board API service — connects strictly to canonical backend endpoints.
 */
const trousseauService = {
  // ── Board Lifecycle ─────────────────────────────────────────────────────────

  createBoard: (data) =>
    api.post('/trousseau', data).then((r) => r.data),

  getUserBoards: () =>
    api.get('/trousseau').then((r) => r.data),

  getBoardById: (boardId) =>
    api.get(`/trousseau/${boardId}`).then((r) => r.data),

  /**
   * Helper to retrieve the current active user board.
   */
  getMyBoard: async () => {
    const listRes = await api.get('/trousseau');
    const boards = listRes.data?.data || listRes.data || [];
    if (!Array.isArray(boards) || boards.length === 0) {
      return null;
    }
    const active = boards.find((b) => b.status === 'ACTIVE') || boards[0];
    if (!active?.id) return null;
    const detailRes = await api.get(`/trousseau/${active.id}`);
    return detailRes.data;
  },

  updateBoard: (boardId, data) =>
    api.put(`/trousseau/${boardId}`, data).then((r) => r.data),

  archiveBoard: (boardId) =>
    api.delete(`/trousseau/${boardId}`).then((r) => r.data),

  // ── Ceremonies ──────────────────────────────────────────────────────────────

  addCeremony: (boardId, data) =>
    api.post(`/trousseau/${boardId}/ceremonies`, data).then((r) => r.data),

  updateCeremony: (boardId, ceremonyId, data) =>
    api.put(`/trousseau/${boardId}/ceremonies/${ceremonyId}`, data).then((r) => r.data),

  removeCeremony: (boardId, ceremonyId) =>
    api.delete(`/trousseau/${boardId}/ceremonies/${ceremonyId}`).then((r) => r.data),

  // ── Items ───────────────────────────────────────────────────────────────────

  addItem: (boardId, ceremonyId, data) =>
    api.post(`/trousseau/${boardId}/ceremonies/${ceremonyId}/items`, data).then((r) => r.data),

  updateItem: (boardId, ceremonyId, itemId, data) =>
    api.patch(`/trousseau/${boardId}/ceremonies/${ceremonyId}/items/${itemId}`, data).then((r) => r.data),

  removeItem: (boardId, ceremonyId, itemId) =>
    api.delete(`/trousseau/${boardId}/ceremonies/${ceremonyId}/items/${itemId}`).then((r) => r.data),

  // ── AI Bridal Curation ──────────────────────────────────────────────────────

  aiCurateCeremony: (boardId, ceremonyId) =>
    api.post(`/trousseau/${boardId}/ceremonies/${ceremonyId}/curate`).then((r) => r.data),

  // ── Collaborators & WhatsApp ────────────────────────────────────────────────

  getCollaborators: (boardId) =>
    api.get(`/trousseau/${boardId}/collaborators`).then((r) => r.data),

  addCollaborator: (boardId, data) =>
    api.post(`/trousseau/${boardId}/collaborators`, data).then((r) => r.data),

  revokeCollaborator: (boardId, collaboratorId) =>
    api.delete(`/trousseau/${boardId}/collaborators/${collaboratorId}`).then((r) => r.data),

  shareViaWhatsApp: (boardId, data) =>
    api.post(`/trousseau/${boardId}/share-whatsapp`, data).then((r) => r.data),

  // ── Public Share & Family Voting ────────────────────────────────────────────

  getSharedView: (token) =>
    api.get(`/trousseau/share/${token}`).then((r) => r.data),

  castVote: (token, itemId, data) =>
    api.post(`/trousseau/share/${token}/items/${itemId}/vote`, data).then((r) => r.data),

  getItemVotes: (token, itemId) =>
    api.get(`/trousseau/share/${token}/items/${itemId}/votes`).then((r) => r.data),

  // ── Cart Conversion ─────────────────────────────────────────────────────────

  convertToCart: (boardId, ceremonyId, itemIds) =>
    api.post(`/trousseau/${boardId}/ceremonies/${ceremonyId}/transfer-to-cart`, { itemIds }).then((r) => r.data),

  convertAllToCart: (boardId) =>
    api.post(`/trousseau/${boardId}/transfer-all-to-cart`).then((r) => r.data),
};

export default trousseauService;
