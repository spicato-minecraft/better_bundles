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

## Planned C2S inner-slot action payload (placeholder)

Not implemented in Phase 1. Reserved shape for server-authoritative overlay edits:

```json
{
  "screenSyncId": "<int>",
  "containerSlotId": "<int>",
  "innerSlot": "<int 0..3>",
  "action": "<enum: PLACE | PICKUP | SWAP>",
  "carriedRevision": "<int>"
}
```

- `screenSyncId`: sync id of the open screen handler (vanilla menu sync id).
- `containerSlotId`: slot index of the bundle stack in the open container (player inventory or external menu).
- `innerSlot`: index into the 2×2 inner grid (0 = top-left, 3 = bottom-right).
- `action`: intended click semantics for the inner slot.
- `carriedRevision`: monotonic revision of the cursor/carried stack so the server can reject stale clicks.

Phase 2+ will define validation, slot policy enforcement (`BundleSlotPolicy`), and server-side application of this payload.
