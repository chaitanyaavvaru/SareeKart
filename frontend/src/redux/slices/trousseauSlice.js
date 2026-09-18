import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import trousseauService from '../../services/trousseauService';

// ── Async Thunks ────────────────────────────────────────────────────────────

export const fetchMyBoard = createAsyncThunk(
  'trousseau/fetchMyBoard',
  async (_, { rejectWithValue }) => {
    try {
      const res = await trousseauService.getMyBoard();
      // res may be { success: true, data: ... } or direct data
      return res?.data !== undefined ? res.data : res;
    } catch (err) {
      if (err.response?.status === 404) {
        return null;
      }
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch trousseau board');
    }
  }
);

export const createBoard = createAsyncThunk(
  'trousseau/createBoard',
  async (boardData, { rejectWithValue }) => {
    try {
      const res = await trousseauService.createBoard(boardData);
      return res?.data !== undefined ? res.data : res;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to create trousseau board');
    }
  }
);

export const updateBoard = createAsyncThunk(
  'trousseau/updateBoard',
  async ({ boardId, data }, { rejectWithValue }) => {
    try {
      const res = await trousseauService.updateBoard(boardId, data);
      return res?.data !== undefined ? res.data : res;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to update board');
    }
  }
);

export const archiveBoard = createAsyncThunk(
  'trousseau/archiveBoard',
  async (boardId, { rejectWithValue }) => {
    try {
      await trousseauService.archiveBoard(boardId);
      return boardId;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to archive board');
    }
  }
);

export const addCeremony = createAsyncThunk(
  'trousseau/addCeremony',
  async ({ boardId, data }, { rejectWithValue }) => {
    try {
      const res = await trousseauService.addCeremony(boardId, data);
      return res?.data !== undefined ? res.data : res;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to add ceremony');
    }
  }
);

export const removeCeremony = createAsyncThunk(
  'trousseau/removeCeremony',
  async ({ boardId, ceremonyId }, { rejectWithValue }) => {
    try {
      await trousseauService.removeCeremony(boardId, ceremonyId);
      return ceremonyId;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to remove ceremony');
    }
  }
);

export const addItem = createAsyncThunk(
  'trousseau/addItem',
  async ({ boardId, ceremonyId, data }, { rejectWithValue }) => {
    try {
      const res = await trousseauService.addItem(boardId, ceremonyId, data);
      const item = res?.data !== undefined ? res.data : res;
      return { ceremonyId, item };
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to add item to ceremony');
    }
  }
);

export const removeItem = createAsyncThunk(
  'trousseau/removeItem',
  async ({ boardId, ceremonyId, itemId }, { rejectWithValue }) => {
    try {
      await trousseauService.removeItem(boardId, ceremonyId, itemId);
      return { ceremonyId, itemId };
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to remove item');
    }
  }
);

export const aiCurateCeremony = createAsyncThunk(
  'trousseau/aiCurateCeremony',
  async ({ boardId, ceremonyId }, { rejectWithValue }) => {
    try {
      const res = await trousseauService.aiCurateCeremony(boardId, ceremonyId);
      const items = res?.data !== undefined ? res.data : res;
      return { ceremonyId, items };
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'AI curation failed');
    }
  }
);

export const addCollaborator = createAsyncThunk(
  'trousseau/addCollaborator',
  async ({ boardId, data }, { rejectWithValue }) => {
    try {
      const res = await trousseauService.addCollaborator(boardId, data);
      return res?.data !== undefined ? res.data : res;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to add collaborator');
    }
  }
);

export const revokeCollaborator = createAsyncThunk(
  'trousseau/revokeCollaborator',
  async ({ boardId, collaboratorId }, { rejectWithValue }) => {
    try {
      await trousseauService.revokeCollaborator(boardId, collaboratorId);
      return collaboratorId;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to revoke collaborator');
    }
  }
);

export const convertToCart = createAsyncThunk(
  'trousseau/convertToCart',
  async ({ boardId, ceremonyId, itemIds }, { rejectWithValue }) => {
    try {
      const res = await trousseauService.convertToCart(boardId, ceremonyId, itemIds);
      return res?.data !== undefined ? res.data : res;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to convert items to cart');
    }
  }
);

// ── Slice Definition ────────────────────────────────────────────────────────

const initialState = {
  board: null,
  activeCeremonyId: null,
  loading: false,
  actionLoading: false,
  aiCurating: false,
  error: null,
  sseConnected: false,
  conversionResult: null,
};

const trousseauSlice = createSlice({
  name: 'trousseau',
  initialState,
  reducers: {
    setActiveCeremony: (state, action) => {
      state.activeCeremonyId = action.payload;
    },
    setSseConnected: (state, action) => {
      state.sseConnected = action.payload;
    },
    clearConversionResult: (state) => {
      state.conversionResult = null;
    },
    clearTrousseauError: (state) => {
      state.error = null;
    },
    applyLiveEvent: (state, action) => {
      const { type, data } = action.payload;
      if (!state.board) return;

      if (type === 'CEREMONY_ADDED') {
        if (!state.board.ceremonies) state.board.ceremonies = [];
        const exists = state.board.ceremonies.some((c) => c.id === data.id);
        if (!exists) {
          state.board.ceremonies.push(data);
          if (!state.activeCeremonyId) {
            state.activeCeremonyId = data.id;
          }
        }
      } else if (type === 'CEREMONY_REMOVED') {
        if (!state.board.ceremonies) return;
        state.board.ceremonies = state.board.ceremonies.filter((c) => c.id !== data.ceremonyId);
        if (state.activeCeremonyId === data.ceremonyId) {
          state.activeCeremonyId = state.board.ceremonies[0]?.id || null;
        }
      } else if (type === 'ITEM_ADDED') {
        const ceremony = (state.board.ceremonies || []).find((c) => c.id === data.ceremonyId);
        if (ceremony) {
          if (!ceremony.items) ceremony.items = [];
          const itemExists = ceremony.items.some((i) => i.id === data.id);
          if (!itemExists) {
            ceremony.items.push(data);
          }
        }
      } else if (type === 'ITEM_REMOVED') {
        const ceremony = (state.board.ceremonies || []).find((c) => c.id === data.ceremonyId);
        if (ceremony && ceremony.items) {
          ceremony.items = ceremony.items.filter((i) => i.id !== data.itemId);
        }
      } else if (type === 'VOTE_CAST') {
        for (const ceremony of state.board.ceremonies || []) {
          const item = (ceremony.items || []).find((i) => i.id === data.itemId);
          if (item) {
            if (data.reaction === 'LOVE') item.loveCount = (item.loveCount || 0) + 1;
            else if (data.reaction === 'LIKE') item.likeCount = (item.likeCount || 0) + 1;
            else if (data.reaction === 'PASS') item.passCount = (item.passCount || 0) + 1;
            if (data.voteCounts) {
              item.voteCounts = data.voteCounts;
            }
            break;
          }
        }
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // fetchMyBoard
      .addCase(fetchMyBoard.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchMyBoard.fulfilled, (state, action) => {
        state.loading = false;
        state.board = action.payload;
        if (action.payload?.ceremonies?.length > 0) {
          if (!state.activeCeremonyId || !action.payload.ceremonies.some((c) => c.id === state.activeCeremonyId)) {
            state.activeCeremonyId = action.payload.ceremonies[0].id;
          }
        } else {
          state.activeCeremonyId = null;
        }
      })
      .addCase(fetchMyBoard.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })

      // createBoard
      .addCase(createBoard.pending, (state) => {
        state.actionLoading = true;
        state.error = null;
      })
      .addCase(createBoard.fulfilled, (state, action) => {
        state.actionLoading = false;
        state.board = action.payload;
        state.activeCeremonyId = action.payload?.ceremonies?.[0]?.id || null;
      })
      .addCase(createBoard.rejected, (state, action) => {
        state.actionLoading = false;
        state.error = action.payload;
      })

      // updateBoard
      .addCase(updateBoard.fulfilled, (state, action) => {
        if (state.board && action.payload) {
          state.board.title = action.payload.title || state.board.title;
          state.board.weddingDate = action.payload.weddingDate || state.board.weddingDate;
          state.board.notes = action.payload.notes || state.board.notes;
        }
      })

      // archiveBoard
      .addCase(archiveBoard.fulfilled, (state) => {
        state.board = null;
        state.activeCeremonyId = null;
      })

      // addCeremony
      .addCase(addCeremony.fulfilled, (state, action) => {
        if (state.board) {
          if (!state.board.ceremonies) state.board.ceremonies = [];
          const exists = state.board.ceremonies.some((c) => c.id === action.payload.id);
          if (!exists) {
            state.board.ceremonies.push(action.payload);
          }
          state.activeCeremonyId = action.payload.id;
        }
      })

      // removeCeremony
      .addCase(removeCeremony.fulfilled, (state, action) => {
        if (state.board && state.board.ceremonies) {
          state.board.ceremonies = state.board.ceremonies.filter((c) => c.id !== action.payload);
          if (state.activeCeremonyId === action.payload) {
            state.activeCeremonyId = state.board.ceremonies[0]?.id || null;
          }
        }
      })

      // addItem
      .addCase(addItem.fulfilled, (state, action) => {
        if (state.board) {
          const ceremony = (state.board.ceremonies || []).find((c) => c.id === action.payload.ceremonyId);
          if (ceremony) {
            if (!ceremony.items) ceremony.items = [];
            const exists = ceremony.items.some((i) => i.id === action.payload.item.id);
            if (!exists) {
              ceremony.items.push(action.payload.item);
            }
          }
        }
      })

      // removeItem
      .addCase(removeItem.fulfilled, (state, action) => {
        if (state.board) {
          const ceremony = (state.board.ceremonies || []).find((c) => c.id === action.payload.ceremonyId);
          if (ceremony && ceremony.items) {
            ceremony.items = ceremony.items.filter((i) => i.id !== action.payload.itemId);
          }
        }
      })

      // aiCurateCeremony
      .addCase(aiCurateCeremony.pending, (state) => {
        state.aiCurating = true;
      })
      .addCase(aiCurateCeremony.fulfilled, (state, action) => {
        state.aiCurating = false;
        if (state.board) {
          const ceremony = (state.board.ceremonies || []).find((c) => c.id === action.payload.ceremonyId);
          if (ceremony) {
            if (!ceremony.items) ceremony.items = [];
            const newItems = Array.isArray(action.payload.items) ? action.payload.items : [];
            for (const it of newItems) {
              if (!ceremony.items.some((existing) => existing.id === it.id)) {
                ceremony.items.push(it);
              }
            }
          }
        }
      })
      .addCase(aiCurateCeremony.rejected, (state, action) => {
        state.aiCurating = false;
        state.error = action.payload;
      })

      // addCollaborator
      .addCase(addCollaborator.fulfilled, (state, action) => {
        if (state.board) {
          if (!state.board.collaborators) state.board.collaborators = [];
          state.board.collaborators.push(action.payload);
        }
      })

      // revokeCollaborator
      .addCase(revokeCollaborator.fulfilled, (state, action) => {
        if (state.board && state.board.collaborators) {
          state.board.collaborators = state.board.collaborators.filter((c) => c.id !== action.payload);
        }
      })

      // convertToCart
      .addCase(convertToCart.pending, (state) => {
        state.actionLoading = true;
      })
      .addCase(convertToCart.fulfilled, (state, action) => {
        state.actionLoading = false;
        state.conversionResult = action.payload;
      })
      .addCase(convertToCart.rejected, (state, action) => {
        state.actionLoading = false;
        state.error = action.payload;
      });
  },
});

export const {
  setActiveCeremony,
  setSseConnected,
  clearConversionResult,
  clearTrousseauError,
  applyLiveEvent,
} = trousseauSlice.actions;

export default trousseauSlice.reducer;
