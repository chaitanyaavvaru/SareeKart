# Milestone 2 Remediation: Error Handling & returnService Standardization in ManageReturns.jsx

## 1. Observation

Direct investigation of `frontend/src/pages/Admin/ManageReturns.jsx` and `frontend/src/services/returnService.js` revealed the following exact observations:

### Observation 1.1: Direct Axios Client Coupling Bypassing `returnService`
In `frontend/src/pages/Admin/ManageReturns.jsx`:
- **Line 19**: Imports Axios directly instead of the service layer:
  ```javascript
  import api from '../../api/axiosConfig';
  ```
- **Line 179 & Line 197**: `fetchClaims` and `useEffect` invoke raw `api.get(url)` rather than `returnService.getAllReturns(activeTab)`.
- **Line 232, 271, 310, 347**: The four moderation action handlers (`handleApprove`, `handleConfirmSchedule`, `handleCompleteRefund`, `handleConfirmReject`) directly invoke `api.put('/admin/returns/${claimId}/status', ...)`.
- No references to `returnService` currently exist in `ManageReturns.jsx`.

### Observation 1.2: Existing Service Layer Implementation
In `frontend/src/services/returnService.js` (Lines 128–156):
```javascript
  /**
   * Fetch all return claims for administrative review.
   * 
   * @param {string} [status='ALL'] - Status filter ('ALL', 'PENDING', 'APPROVED', 'PICKUP_SCHEDULED', 'COMPLETED', 'REJECTED')
   * @returns {Promise<Object>} API response body containing list of ReturnResponse
   */
  getAllReturns: async (status = 'ALL') => {
    const url = status && status !== 'ALL' ? `/admin/returns?status=${status}` : '/admin/returns';
    const response = await api.get(url);
    return response.data;
  },

  /**
   * Update the moderation status of a return claim.
   * 
   * @param {number|string} id - Return claim ID
   * @param {Object} updateData
   * @param {('APPROVED'|'PICKUP_SCHEDULED'|'REJECTED'|'COMPLETED')} updateData.status - Target status
   * @param {string} [updateData.reverseCourier] - Mandatory if status is PICKUP_SCHEDULED
   * @param {string} [updateData.reverseTrackingNumber] - Mandatory if status is PICKUP_SCHEDULED
   * @param {string} [updateData.adminNotes] - Mandatory if status is REJECTED
   * @param {number} [updateData.refundAmount] - Optional adjusted refund amount
   * @returns {Promise<Object>} API response body containing updated ReturnResponse
   */
  updateReturnStatus: async (id, updateData) => {
    const response = await api.put(`/admin/returns/${id}/status`, updateData);
    return response.data;
  },
```
Notice that `returnService.updateReturnStatus` returns `response.data` (the unwrapped backend `ApiResponse` payload).

### Observation 1.3: Error Swallowing and Optimistic Local State Overwrite
In `frontend/src/pages/Admin/ManageReturns.jsx`, all four action handlers catch errors and unconditionally mutate local state as if the operation succeeded, displaying false positive success alerts:

1. **`handleApprove` (Lines 243–248)**:
   ```javascript
       } catch {
         // Local optimistic update if backend returns 404/network error
         setClaims((prev) => prev.map((c) => (c.id === claimId ? { ...c, status: 'APPROVED' } : c)));
         setSuccessMsg(`Claim #RET-${claimId} status updated to APPROVED.`);
       } finally {
         setActionLoadingId(null);
       }
   ```
2. **`handleConfirmSchedule` (Lines 290–300)**:
   ```javascript
       } catch {
         setClaims((prev) => prev.map((c) => (c.id === claimId ? {
           ...c,
           status: 'PICKUP_SCHEDULED',
           reverseCourier: courierPartner,
           reverseTrackingNumber: trackingNumber.trim(),
           adminNotes: pickupNotes.trim()
         } : c)));
         setSuccessMsg(`Pickup scheduled for #RET-${claimId}.`);
         setScheduleModalClaim(null);
       } finally {
         setActionLoadingId(null);
       }
   ```
3. **`handleCompleteRefund` (Lines 322–327)**:
   ```javascript
       } catch {
         setClaims((prev) => prev.map((c) => (c.id === claimId ? { ...c, status: 'COMPLETED' } : c)));
         setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
       } finally {
         setActionLoadingId(null);
       }
   ```
4. **`handleConfirmReject` (Lines 359–366)**:
   ```javascript
       } catch {
         setClaims((prev) => prev.map((c) => (c.id === claimId ? {
           ...c,
           status: 'REJECTED',
           adminNotes: rejectionReason.trim()
         } : c)));
         setSuccessMsg(`Claim #RET-${claimId} rejected.`);
         setRejectModalClaim(null);
       } finally {
         setActionLoadingId(null);
       }
   ```

### Observation 1.4: Backend Error Response Structure
- In `backend/backend/src/main/java/com/sareekart/exception/GlobalExceptionHandler.java`:
  All exceptions (`ResourceNotFoundException`, `BadRequestException`, `AccessDeniedException`, etc.) return `ApiResponse.error(message)` with HTTP status 400, 403, 404, or 500:
  `{ "success": false, "message": "<detailed reason>", "data": null }`
- When Axios rejects, the error response payload is accessible via `err.response?.data?.message`.
- If unauthenticated or forbidden (HTTP 403), `err.response?.data?.message` contains `"Not authorised to perform this action"`.
- If bad request (HTTP 400), `err.response?.data?.message` contains the exact domain constraint failure (e.g., `"Invalid state transition from PENDING to COMPLETED"`, `"Mandatory rejection reason must be provided in admin notes"`).

---

## 2. Logic Chain

1. **Service Abstraction Consistency (from Obs 1.1, Obs 1.2)**:
   - `ReturnRequestModal.jsx` and `MyOrders.jsx` both route all return-related API calls through `returnService.js`.
   - `returnService.js` already exports `getAllReturns` and `updateReturnStatus`.
   - Direct Axios calls in `ManageReturns.jsx` violate architectural consistency and bypass any centralized interceptors, logging, or transformations provided by the service layer.
   - Therefore, replacing `api` with `returnService` in `ManageReturns.jsx` aligns the admin console with the rest of the application.

2. **Security & State Integrity (from Obs 1.3, Obs 1.4)**:
   - The comment `// Local optimistic update if backend returns 404/network error` reflects mock prototype behavior that violates production safety.
   - When an unauthorized user (or expired JWT, or invalid transition) triggers an action, the backend rejects the request with HTTP 4xx.
   - The existing code catches this rejection, suppresses `err`, changes the in-memory React state to the requested status, and outputs a green banner claiming success.
   - Upon page refresh, the user discovers the database never updated, resulting in state divergence and severe confusion.
   - Worse, security boundaries (RBAC 403 Forbidden) are visually bypassed on the client.
   - Therefore, optimistic updates and success banners in `catch` blocks must be completely eliminated.

3. **User Feedback Protocol (from Obs 1.3, Obs 1.4)**:
   - When `updateReturnStatus` fails, `catch (err)` must extract `err.response?.data?.message || 'Action failed'` and pass it to `setErrorMsg(...)`.
   - Any active `successMsg` must be cleared (`setSuccessMsg(null)`).
   - In-memory `claims` state must remain untouched so the true database status remains visible.
   - For `handleConfirmSchedule` and `handleConfirmReject`, modals should either display the error or allow retry without falsely closing on error. Rendering `{errorMsg && ...}` inside the modals provides immediate visibility even with the modal backdrop active.

---

## 3. Caveats

1. **Offline Mock Fallback on Initial Load**:
   - `ManageReturns.jsx` retains a fallback to `MOCK_ADMIN_RETURNS` in `fetchClaims` when the backend is completely offline (`catch (err)` in `fetchClaims`). This read-only mock fallback is intentional for offline preview testing and does NOT apply to action handlers.
2. **Return Value Unwrapping**:
   - Because `returnService.updateReturnStatus` returns `response.data`, the returned object `res` is the backend `ApiResponse` (`{ success: true, message: "...", data: {...} }`). Therefore, code checking `res.data?.success` must check `res?.success` (or `res?.success || res.data?.success` for defensive compatibility).
3. **Source Code Write Permission**:
   - As an Explorer agent, no changes have been applied to `ManageReturns.jsx`. All changes below are formulated as precise before/after specifications and patch snippets for the implementer.

---

## 4. Conclusion & Proposed Fixes

### Target File
`frontend/src/pages/Admin/ManageReturns.jsx`

### Proposed Changes Summary
1. Replace `import api from '../../api/axiosConfig';` with `import returnService from '../../services/returnService';`.
2. Refactor `fetchClaims` to use `returnService.getAllReturns(activeTab)`.
3. Simplify `useEffect` to trigger `fetchClaims()`.
4. Refactor `handleApprove` to:
   - Call `returnService.updateReturnStatus(claimId, { status: 'APPROVED', adminNotes: ... })`.
   - On success (`res?.success`): set success banner, refresh claims, update inspection claim if active.
   - On error: set `setErrorMsg(err.response?.data?.message || 'Action failed')`, clear `successMsg`, DO NOT update `claims`.
5. Refactor `handleConfirmSchedule` to:
   - Call `returnService.updateReturnStatus(claimId, { status: 'PICKUP_SCHEDULED', reverseCourier, reverseTrackingNumber, adminNotes })`.
   - On success: set success banner, close schedule modal, refresh claims, update inspection claim if active.
   - On error: set `setErrorMsg(err.response?.data?.message || 'Action failed')`, DO NOT update `claims`, DO NOT set success banner.
6. Refactor `handleCompleteRefund` to:
   - Call `returnService.updateReturnStatus(claimId, { status: 'COMPLETED', refundAmount, adminNotes })`.
   - On success: set success banner, refresh claims, update inspection claim if active.
   - On error: set `setErrorMsg(err.response?.data?.message || 'Action failed')`, DO NOT update `claims`, DO NOT set success banner.
7. Refactor `handleConfirmReject` to:
   - Call `returnService.updateReturnStatus(claimId, { status: 'REJECTED', adminNotes: rejectionReason.trim() })`.
   - On success: set success banner, close rejection modal, refresh claims, update inspection claim if active.
   - On error: set `setErrorMsg(err.response?.data?.message || 'Action failed')`, DO NOT update `claims`, DO NOT set success banner.
8. Render error banner `{errorMsg && ...}` inside Schedule Modal, Reject Modal, and Inspection Lightbox so staff can see errors without closing modals.

---

### Detailed Before / After Code Specifications

#### Change 1: Imports (Lines 18–20)
**Before**:
```javascript
} from 'lucide-react';
import api from '../../api/axiosConfig';
import SEO from '../../components/common/SEO';
```
**After**:
```javascript
} from 'lucide-react';
import returnService from '../../services/returnService';
import SEO from '../../components/common/SEO';
```

---

#### Change 2: `fetchClaims` and `useEffect` (Lines 174–219)
**Before**:
```javascript
  const fetchClaims = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const url = activeTab === 'ALL' ? '/admin/returns' : `/admin/returns?status=${activeTab}`;
      const res = await api.get(url);
      if (res.data?.success && Array.isArray(res.data.data)) {
        setClaims(res.data.data);
      } else {
        // Resilient fallback
        setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
      }
    } catch (err) {
      console.warn('Backend endpoint unavailable, using offline fallback claims', err);
      setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
    } finally {
      setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    let isMounted = true;
    const url = activeTab === 'ALL' ? '/admin/returns' : `/admin/returns?status=${activeTab}`;
    api.get(url)
      .then((res) => {
        if (isMounted) {
          if (res.data?.success && Array.isArray(res.data.data)) {
            setClaims(res.data.data);
          } else {
            setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
          }
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.warn('Backend endpoint unavailable, using offline fallback claims', err);
          setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
          setLoading(false);
        }
      });
    return () => {
      isMounted = false;
    };
  }, [activeTab]);
```

**After**:
```javascript
  const fetchClaims = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const res = await returnService.getAllReturns(activeTab);
      if (res?.success && Array.isArray(res.data)) {
        setClaims(res.data);
      } else if (Array.isArray(res)) {
        setClaims(res);
      } else {
        // Resilient fallback
        setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
      }
    } catch (err) {
      console.warn('Backend endpoint unavailable, using offline fallback claims', err);
      setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
    } finally {
      setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchClaims();
  }, [fetchClaims]);
```

---

#### Change 3: Action Handlers (Lines 227–370)
**Before**:
```javascript
  // 1-Click Action: Approve Return
  const handleApprove = async (claimId) => {
    setActionLoadingId(claimId);
    setErrorMsg(null);
    try {
      const res = await api.put(`/admin/returns/${claimId}/status`, {
        status: 'APPROVED',
        adminNotes: 'Condition verified from photographs. Approved for reverse pickup.'
      });
      if (res.data?.success) {
        setSuccessMsg(`Return claim #RET-${claimId} has been approved.`);
        fetchClaims();
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'APPROVED' }));
        }
      }
    } catch {
      // Local optimistic update if backend returns 404/network error
      setClaims((prev) => prev.map((c) => (c.id === claimId ? { ...c, status: 'APPROVED' } : c)));
      setSuccessMsg(`Claim #RET-${claimId} status updated to APPROVED.`);
    } finally {
      setActionLoadingId(null);
    }
  };

  // Open Schedule Pickup Modal
  const openScheduleModal = (claim) => {
    setScheduleModalClaim(claim);
    setCourierPartner('Blue Dart Reverse Logistics');
    setTrackingNumber(`BDR-RET-${claim.orderId}-${Date.now().toString().slice(-4)}`);
    setPickupNotes('Doorstep reverse pickup scheduled. Pack in original box with tags intact.');
  };

  // Submit Schedule Pickup
  const handleConfirmSchedule = async () => {
    if (!scheduleModalClaim) return;
    if (!trackingNumber.trim()) {
      setErrorMsg('Please provide a valid reverse AWB tracking number.');
      return;
    }
    const claimId = scheduleModalClaim.id;
    setActionLoadingId(claimId);
    setErrorMsg(null);
    try {
      const res = await api.put(`/admin/returns/${claimId}/status`, {
        status: 'PICKUP_SCHEDULED',
        reverseCourier: courierPartner,
        reverseTrackingNumber: trackingNumber.trim(),
        adminNotes: pickupNotes.trim()
      });
      if (res.data?.success) {
        setSuccessMsg(`Reverse pickup scheduled for claim #RET-${claimId}. AWB: ${trackingNumber}`);
        fetchClaims();
        setScheduleModalClaim(null);
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({
            ...prev,
            status: 'PICKUP_SCHEDULED',
            reverseCourier: courierPartner,
            reverseTrackingNumber: trackingNumber.trim()
          }));
        }
      }
    } catch {
      setClaims((prev) => prev.map((c) => (c.id === claimId ? {
        ...c,
        status: 'PICKUP_SCHEDULED',
        reverseCourier: courierPartner,
        reverseTrackingNumber: trackingNumber.trim(),
        adminNotes: pickupNotes.trim()
      } : c)));
      setSuccessMsg(`Pickup scheduled for #RET-${claimId}.`);
      setScheduleModalClaim(null);
    } finally {
      setActionLoadingId(null);
    }
  };

  // 1-Click Action: Complete Refund
  const handleCompleteRefund = async (claimId, refundAmount) => {
    setActionLoadingId(claimId);
    setErrorMsg(null);
    try {
      const res = await api.put(`/admin/returns/${claimId}/status`, {
        status: 'COMPLETED',
        refundAmount: refundAmount || 0,
        adminNotes: 'Item received at central hub, physical inspection passed, refund processed.'
      });
      if (res.data?.success) {
        setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
        fetchClaims();
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'COMPLETED' }));
        }
      }
    } catch {
      setClaims((prev) => prev.map((c) => (c.id === claimId ? { ...c, status: 'COMPLETED' } : c)));
      setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
    } finally {
      setActionLoadingId(null);
    }
  };

  // Open Rejection Modal
  const openRejectModal = (claim) => {
    setRejectModalClaim(claim);
    setRejectionReason('');
  };

  // Submit Rejection
  const handleConfirmReject = async () => {
    if (!rejectModalClaim) return;
    if (!rejectionReason.trim()) {
      setErrorMsg('Mandatory rejection reason must be provided.');
      return;
    }
    const claimId = rejectModalClaim.id;
    setActionLoadingId(claimId);
    setErrorMsg(null);
    try {
      const res = await api.put(`/admin/returns/${claimId}/status`, {
        status: 'REJECTED',
        adminNotes: rejectionReason.trim()
      });
      if (res.data?.success) {
        setSuccessMsg(`Claim #RET-${claimId} has been rejected.`);
        fetchClaims();
        setRejectModalClaim(null);
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'REJECTED', adminNotes: rejectionReason.trim() }));
        }
      }
    } catch {
      setClaims((prev) => prev.map((c) => (c.id === claimId ? {
        ...c,
        status: 'REJECTED',
        adminNotes: rejectionReason.trim()
      } : c)));
      setSuccessMsg(`Claim #RET-${claimId} rejected.`);
      setRejectModalClaim(null);
    } finally {
      setActionLoadingId(null);
    }
  };
```

**After**:
```javascript
  // 1-Click Action: Approve Return
  const handleApprove = async (claimId) => {
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'APPROVED',
        adminNotes: 'Condition verified from photographs. Approved for reverse pickup.'
      });
      if (res?.success) {
        setSuccessMsg(`Return claim #RET-${claimId} has been approved.`);
        fetchClaims();
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'APPROVED' }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Open Schedule Pickup Modal
  const openScheduleModal = (claim) => {
    setScheduleModalClaim(claim);
    setErrorMsg(null);
    setCourierPartner('Blue Dart Reverse Logistics');
    setTrackingNumber(`BDR-RET-${claim.orderId}-${Date.now().toString().slice(-4)}`);
    setPickupNotes('Doorstep reverse pickup scheduled. Pack in original box with tags intact.');
  };

  // Submit Schedule Pickup
  const handleConfirmSchedule = async () => {
    if (!scheduleModalClaim) return;
    if (!trackingNumber.trim()) {
      setErrorMsg('Please provide a valid reverse AWB tracking number.');
      return;
    }
    const claimId = scheduleModalClaim.id;
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'PICKUP_SCHEDULED',
        reverseCourier: courierPartner,
        reverseTrackingNumber: trackingNumber.trim(),
        adminNotes: pickupNotes.trim()
      });
      if (res?.success) {
        setSuccessMsg(`Reverse pickup scheduled for claim #RET-${claimId}. AWB: ${trackingNumber}`);
        fetchClaims();
        setScheduleModalClaim(null);
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({
            ...prev,
            status: 'PICKUP_SCHEDULED',
            reverseCourier: courierPartner,
            reverseTrackingNumber: trackingNumber.trim(),
            adminNotes: pickupNotes.trim()
          }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // 1-Click Action: Complete Refund
  const handleCompleteRefund = async (claimId, refundAmount) => {
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'COMPLETED',
        refundAmount: refundAmount || 0,
        adminNotes: 'Item received at central hub, physical inspection passed, refund processed.'
      });
      if (res?.success) {
        setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
        fetchClaims();
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'COMPLETED' }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Open Rejection Modal
  const openRejectModal = (claim) => {
    setRejectModalClaim(claim);
    setErrorMsg(null);
    setRejectionReason('');
  };

  // Submit Rejection
  const handleConfirmReject = async () => {
    if (!rejectModalClaim) return;
    if (!rejectionReason.trim()) {
      setErrorMsg('Mandatory rejection reason must be provided.');
      return;
    }
    const claimId = rejectModalClaim.id;
    setActionLoadingId(claimId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const res = await returnService.updateReturnStatus(claimId, {
        status: 'REJECTED',
        adminNotes: rejectionReason.trim()
      });
      if (res?.success) {
        setSuccessMsg(`Claim #RET-${claimId} has been rejected.`);
        fetchClaims();
        setRejectModalClaim(null);
        if (inspectClaim?.id === claimId) {
          setInspectClaim((prev) => ({ ...prev, status: 'REJECTED', adminNotes: rejectionReason.trim() }));
        }
      } else {
        setErrorMsg(res?.message || 'Action failed');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };
```

---

#### Change 4: Inline Modal Error Display
To ensure errors are immediately visible to staff when a modal or inspection drawer is active (since modals have `fixed inset-0 z-50` with darkened backdrops):

1. **In Inspection Lightbox (Above line 1013)**:
```jsx
                {/* Inline Action Controls Inside Lightbox */}
                <div className="pt-4 border-t border-[#DDD8CF] space-y-2">
                  {errorMsg && (
                    <div className="p-2.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                      <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                      <span>{errorMsg}</span>
                    </div>
                  )}
                  <p className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                    Moderation Actions
                  </p>
```

2. **In Schedule Pickup Modal (Below line 1110)**:
```jsx
            <p className="text-xs text-[#71817A]">
              Assign reverse logistics partner and AWB tracking for claim <strong className="text-[#17211F]">#RET-{scheduleModalClaim.id}</strong> (Order #SK-{scheduleModalClaim.orderId}).
            </p>

            {errorMsg && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}
```

3. **In Reject Return Modal (Below line 1216)**:
```jsx
            <p className="text-xs text-rose-800 bg-rose-50 border border-rose-200 p-2.5 rounded-xl font-medium">
              Mandatory: A clear justification reason must be specified. This explanation will be permanently recorded and displayed directly on the customer&apos;s order card.
            </p>

            {errorMsg && (
              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}
```

---

## 5. Verification Method

### Step 5.1: Static Code Inspection
1. Inspect `frontend/src/pages/Admin/ManageReturns.jsx`:
   - Verify `api` is no longer imported (`import api from '../../api/axiosConfig'`).
   - Verify `returnService` is imported (`import returnService from '../../services/returnService'`).
   - Confirm lines 243-248, 290-300, 322-327, 359-366 no longer contain `setClaims(...)` or `setSuccessMsg(...)` in `catch` blocks.
   - Confirm all four handlers have `setErrorMsg(err.response?.data?.message || 'Action failed')`.

### Step 5.2: Production Build & Bundle Budget Verification
Run the frontend production build:
```bash
cd frontend && npm run build
```
**Expected Result**:
- Vite build completes with exit code 0.
- `ManageReturns` chunk is under 50 kB (budget is 500 kB).

### Step 5.3: Error Handling Functional Verification
1. **403 Forbidden Simulation**:
   - Send unauthorized request to `PUT /api/admin/returns/101/status` (or simulate 403 via mock interceptor).
   - Expected behavior:
     - Error banner renders: `"Not authorised to perform this action"`.
     - Claim #RET-101 in the table remains in its original status (`PENDING`).
     - No green success banner is shown.
2. **400 Bad Request Simulation**:
   - Send invalid payload (e.g. empty rejection reason or illegal transition).
   - Expected behavior:
     - Error banner renders the backend message (e.g. `"Mandatory rejection reason must be provided in admin notes."`).
     - Local claim status remains unchanged.
3. **Success State Confirmation**:
   - When API returns 200 OK with `{ success: true, data: { status: 'APPROVED', ... } }`:
     - Success banner renders (`"Return claim #RET-101 has been approved."`).
     - Table updates.

---

## 6. Ready-to-Apply Patch

Save as `manage_returns_error_handling.patch` or apply directly:

```patch
--- a/frontend/src/pages/Admin/ManageReturns.jsx
+++ b/frontend/src/pages/Admin/ManageReturns.jsx
@@ -16,7 +16,7 @@ import { 
   ChevronLeft,
   X, 
 } from 'lucide-react';
-import api from '../../api/axiosConfig';
+import returnService from '../../services/returnService';
 import SEO from '../../components/common/SEO';
 
 // Resilient Offline Mock Claims Fallback
@@ -176,9 +176,10 @@ export default function ManageReturns() {
     setLoading(true);
     setErrorMsg(null);
     try {
-      const url = activeTab === 'ALL' ? '/admin/returns' : `/admin/returns?status=${activeTab}`;
-      const res = await api.get(url);
-      if (res.data?.success && Array.isArray(res.data.data)) {
+      const res = await returnService.getAllReturns(activeTab);
+      if (res?.success && Array.isArray(res.data)) {
+        setClaims(res.data);
+      } else if (Array.isArray(res)) {
         setClaims(res.data);
       } else {
         // Resilient fallback
@@ -192,27 +193,7 @@ export default function ManageReturns() {
   }, [activeTab]);
 
   useEffect(() => {
-    let isMounted = true;
-    const url = activeTab === 'ALL' ? '/admin/returns' : `/admin/returns?status=${activeTab}`;
-    api.get(url)
-      .then((res) => {
-        if (isMounted) {
-          if (res.data?.success && Array.isArray(res.data.data)) {
-            setClaims(res.data.data);
-          } else {
-            setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
-          }
-          setLoading(false);
-        }
-      })
-      .catch((err) => {
-        if (isMounted) {
-          console.warn('Backend endpoint unavailable, using offline fallback claims', err);
-          setClaims(MOCK_ADMIN_RETURNS.filter((c) => activeTab === 'ALL' || c.status === activeTab));
-          setLoading(false);
-        }
-      });
-    return () => {
-      isMounted = false;
-    };
-  }, [activeTab]);
+    fetchClaims();
+  }, [fetchClaims]);
 
   // Copy AWB utility
@@ -228,21 +209,24 @@ export default function ManageReturns() {
   const handleApprove = async (claimId) => {
     setActionLoadingId(claimId);
     setErrorMsg(null);
+    setSuccessMsg(null);
     try {
-      const res = await api.put(`/admin/returns/${claimId}/status`, {
+      const res = await returnService.updateReturnStatus(claimId, {
         status: 'APPROVED',
         adminNotes: 'Condition verified from photographs. Approved for reverse pickup.'
       });
-      if (res.data?.success) {
+      if (res?.success) {
         setSuccessMsg(`Return claim #RET-${claimId} has been approved.`);
         fetchClaims();
         if (inspectClaim?.id === claimId) {
           setInspectClaim((prev) => ({ ...prev, status: 'APPROVED' }));
         }
+      } else {
+        setErrorMsg(res?.message || 'Action failed');
       }
-    } catch {
-      // Local optimistic update if backend returns 404/network error
-      setClaims((prev) => prev.map((c) => (c.id === claimId ? { ...c, status: 'APPROVED' } : c)));
-      setSuccessMsg(`Claim #RET-${claimId} status updated to APPROVED.`);
+    } catch (err) {
+      setErrorMsg(err.response?.data?.message || 'Action failed');
     } finally {
       setActionLoadingId(null);
     }
@@ -253,6 +237,7 @@ export default function ManageReturns() {
   const openScheduleModal = (claim) => {
     setScheduleModalClaim(claim);
+    setErrorMsg(null);
     setCourierPartner('Blue Dart Reverse Logistics');
     setTrackingNumber(`BDR-RET-${claim.orderId}-${Date.now().toString().slice(-4)}`);
     setPickupNotes('Doorstep reverse pickup scheduled. Pack in original box with tags intact.');
@@ -267,8 +252,9 @@ export default function ManageReturns() {
     const claimId = scheduleModalClaim.id;
     setActionLoadingId(claimId);
     setErrorMsg(null);
+    setSuccessMsg(null);
     try {
-      const res = await api.put(`/admin/returns/${claimId}/status`, {
+      const res = await returnService.updateReturnStatus(claimId, {
         status: 'PICKUP_SCHEDULED',
         reverseCourier: courierPartner,
         reverseTrackingNumber: trackingNumber.trim(),
@@ -276,7 +262,7 @@ export default function ManageReturns() {
       });
-      if (res.data?.success) {
+      if (res?.success) {
         setSuccessMsg(`Reverse pickup scheduled for claim #RET-${claimId}. AWB: ${trackingNumber}`);
         fetchClaims();
         setScheduleModalClaim(null);
@@ -288,15 +274,10 @@ export default function ManageReturns() {
           }));
         }
+      } else {
+        setErrorMsg(res?.message || 'Action failed');
       }
-    } catch {
-      setClaims((prev) => prev.map((c) => (c.id === claimId ? {
-        ...c,
-        status: 'PICKUP_SCHEDULED',
-        reverseCourier: courierPartner,
-        reverseTrackingNumber: trackingNumber.trim(),
-        adminNotes: pickupNotes.trim()
-      } : c)));
-      setSuccessMsg(`Pickup scheduled for #RET-${claimId}.`);
-      setScheduleModalClaim(null);
+    } catch (err) {
+      setErrorMsg(err.response?.data?.message || 'Action failed');
     } finally {
       setActionLoadingId(null);
     }
@@ -307,21 +288,24 @@ export default function ManageReturns() {
   const handleCompleteRefund = async (claimId, refundAmount) => {
     setActionLoadingId(claimId);
     setErrorMsg(null);
+    setSuccessMsg(null);
     try {
-      const res = await api.put(`/admin/returns/${claimId}/status`, {
+      const res = await returnService.updateReturnStatus(claimId, {
         status: 'COMPLETED',
         refundAmount: refundAmount || 0,
         adminNotes: 'Item received at central hub, physical inspection passed, refund processed.'
       });
-      if (res.data?.success) {
+      if (res?.success) {
         setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
         fetchClaims();
         if (inspectClaim?.id === claimId) {
           setInspectClaim((prev) => ({ ...prev, status: 'COMPLETED' }));
         }
+      } else {
+        setErrorMsg(res?.message || 'Action failed');
       }
-    } catch {
-      setClaims((prev) => prev.map((c) => (c.id === claimId ? { ...c, status: 'COMPLETED' } : c)));
-      setSuccessMsg(`Return claim #RET-${claimId} marked COMPLETED.`);
+    } catch (err) {
+      setErrorMsg(err.response?.data?.message || 'Action failed');
     } finally {
       setActionLoadingId(null);
     }
@@ -332,6 +316,7 @@ export default function ManageReturns() {
   const openRejectModal = (claim) => {
     setRejectModalClaim(claim);
+    setErrorMsg(null);
     setRejectionReason('');
   };
 
@@ -344,8 +329,9 @@ export default function ManageReturns() {
     const claimId = rejectModalClaim.id;
     setActionLoadingId(claimId);
     setErrorMsg(null);
+    setSuccessMsg(null);
     try {
-      const res = await api.put(`/admin/returns/${claimId}/status`, {
+      const res = await returnService.updateReturnStatus(claimId, {
         status: 'REJECTED',
         adminNotes: rejectionReason.trim()
       });
-      if (res.data?.success) {
+      if (res?.success) {
         setSuccessMsg(`Claim #RET-${claimId} has been rejected.`);
         fetchClaims();
         setRejectModalClaim(null);
         if (inspectClaim?.id === claimId) {
           setInspectClaim((prev) => ({ ...prev, status: 'REJECTED', adminNotes: rejectionReason.trim() }));
         }
+      } else {
+        setErrorMsg(res?.message || 'Action failed');
       }
-    } catch {
-      setClaims((prev) => prev.map((c) => (c.id === claimId ? {
-        ...c,
-        status: 'REJECTED',
-        adminNotes: rejectionReason.trim()
-      } : c)));
-      setSuccessMsg(`Claim #RET-${claimId} rejected.`);
-      setRejectModalClaim(null);
+    } catch (err) {
+      setErrorMsg(err.response?.data?.message || 'Action failed');
     } finally {
       setActionLoadingId(null);
     }
@@ -1014,6 +994,12 @@ export default function ManageReturns() {
                 <div className="pt-4 border-t border-[#DDD8CF] space-y-2">
+                  {errorMsg && (
+                    <div className="p-2.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
+                      <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
+                      <span>{errorMsg}</span>
+                    </div>
+                  )}
                   <p className="text-[10px] font-bold uppercase tracking-wider text-[#71817A]">
                     Moderation Actions
                   </p>
@@ -1109,6 +1095,13 @@ export default function ManageReturns() {
               Assign reverse logistics partner and AWB tracking for claim <strong className="text-[#17211F]">#RET-{scheduleModalClaim.id}</strong> (Order #SK-{scheduleModalClaim.orderId}).
             </p>
 
+            {errorMsg && (
+              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
+                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
+                <span>{errorMsg}</span>
+              </div>
+            )}
+
             <div className="space-y-3 text-xs">
               {/* Courier Partner Selection */}
@@ -1215,6 +1208,13 @@ export default function ManageReturns() {
               Mandatory: A clear justification reason must be specified. This explanation will be permanently recorded and displayed directly on the customer&apos;s order card.
             </p>
 
+            {errorMsg && (
+              <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center gap-2">
+                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
+                <span>{errorMsg}</span>
+              </div>
+            )}
+
             {/* Quick Policy Chips */}
             <div>
```
