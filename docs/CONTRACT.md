# Better Bundles Contract (Lane A Phase 1)

Frozen public surface for bundle inner-inventory storage and the planned client sync packet.

## BundleInventoryAccess

Target items: any stack whose item is a vanilla `BundleItem` (default and dyed bundles).

| Method | Contract |
| --- | --- |
| `boolean isBundleItem(ItemStack stack)` | Returns `true` when `stack` is non-empty and its item is a `BundleItem`. |
| `BundleInventory read(ItemStack stack)` | Returns the four-slot inner inventory. On first read for an unmigrated bundle, copies vanilla `DataComponents.BUNDLE_CONTENTS` into slots 0→3 in iteration order (extras beyond four slots are dropped), sets `better_bundles:bundle_contents` with `migrated: true`, and removes vanilla `BUNDLE_CONTENTS`. Non-bundle stacks return `BundleInventory.empty()` without mutation. |
| `void write(ItemStack stack, BundleInventory inventory)` | Writes `better_bundles:bundle_contents` with `migrated: true` and removes vanilla `BUNDLE_CONTENTS`. No-op for non-bundle stacks. |

## Data component: `better_bundles:bundle_contents`

Codec shape (persistent):

```json
{
  "slot0": "<ItemStack>",
  "slot1": "<ItemStack>",
  "slot2": "<ItemStack>",
  "slot3": "<ItemStack>",
  "migrated": true
}
```

Maps to `BundleInventory` via the four slot fields; `migrated` tracks one-time conversion from vanilla storage.

## C2S inner-slot action payload

Server-authoritative overlay edits. Registered in `BetterBundles` via `PayloadTypeRegistry.playC2S()` and handled by `BundleOverlaySlotHandler` (Phase 2b — server only; no client send yet).

**Payload id:** `better_bundles:bundle_slot_action`

Wire shape (fields in order):

```json
{
  "screenSyncId": "<varint>",
  "containerSlotId": "<varint>",
  "innerSlot": "<varint 0..3>",
  "action": "<varint enum ordinal>",
  "carriedRevision": "<varint>",
  "sourceSlotId": "<varint, -1 when unused>"
}
```

| Field | Semantics |
| --- | --- |
| `screenSyncId` | Must equal the open menu's `AbstractContainerMenu#getStateId()` when the action is applied. |
| `containerSlotId` | Slot index of the bundle stack in the open container (player inventory or external menu). |
| `innerSlot` | Index into the 2×2 inner grid (0 = top-left, 3 = bottom-right). |
| `action` | `0` PLACE, `1` PICKUP, `2` SWAP, `3` SHIFT_MOVE, `4` MERGE |
| `carriedRevision` | Must equal the menu's current `getStateId()` so stale cursor clicks are rejected. |
| `sourceSlotId` | `-1` when unused. For SHIFT_MOVE into bundle, the player slot index to pull from. |

### Validation (server)

1. Menu `stillValid(player)`.
2. `screenSyncId == menu.getStateId()`.
3. `carriedRevision == menu.getStateId()`.
4. `containerSlotId` is a valid slot index in the open menu.
5. Item in `containerSlotId` passes `BundleInventoryAccess.isBundleItem`.
6. `innerSlot` in `0..3`.
7. `sourceSlotId` is `-1` unless `SHIFT_MOVE` moves from a player slot into the bundle.
8. `SHIFT_MOVE` and `MERGE` are implemented in Phase 5.

### Action semantics

All mutations use `BundleInventoryAccess.read` / `write` and enforce `BundleSlotPolicy.canInsert` for items entering the bundle.

| Action | Preconditions | Effect |
| --- | --- | --- |
| PLACE | Carried non-empty; inner slot empty; `canInsert(carried)` | Copies carried into inner slot; clears carried. |
| PICKUP | Carried empty; inner slot non-empty | Moves inner stack to carried; clears inner slot. |
| SWAP | Both carried and inner non-empty; `canInsert(carried)` | Exchanges carried and inner stacks. |
| SHIFT_MOVE | `sourceSlotId == -1`: inner slot non-empty. `sourceSlotId >= 0`: player slot non-empty, bundle has empty inner slot, `canInsert(source)` | Out: quick-moves inner stack into player inventory. In: moves player slot stack into first empty inner slot. |
| MERGE | Carried and inner same item; inner below max stack | Merges carried count into inner up to max stack size. |

On success: bundle stack is written back to `containerSlotId`, carried is updated, the container slot is marked changed, and `broadcastChanges()` runs.
