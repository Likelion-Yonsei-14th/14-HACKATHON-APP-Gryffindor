package com.gryffindor.smartshopping.data.meta

/**
 * Task 6.2: Lifecycle and single cleanup behavior.
 *
 * NOT IMPLEMENTED as unit tests for the following reason:
 *
 * MetaCameraSource directly instantiates Meta DAT SDK objects (Wearables, DeviceSession,
 * Camera, etc.) inside its methods. The DAT SDK does not provide test doubles or a
 * factory interface that would allow injecting fakes without fundamentally restructuring
 * the adapter.
 *
 * Per task spec: "DAT SDK 특성상 합리적인 test seam 없이 unit test를 만들기 위해
 * production architecture를 크게 변경해야 한다면 그렇게 하지 마라."
 *
 * The following behaviors are instead verified during Task 8 (Mock Device E2E):
 * - start → Streaming → stop → NotConnected → fresh start → Streaming
 * - fresh start creates a new DeviceSession
 * - startup failure triggers cleanup
 * - stream failure triggers cleanup
 * - explicit stop and coroutine cancellation do not double-teardown
 * - repeated stop does not crash
 *
 * The bounded delivery and failure isolation behaviors ARE tested in:
 * - BoundedFrameDeliveryTest (Task 6.3)
 * - ShoppingFlowIsolationTest (Task 6.4)
 */
object MetaCameraSourceLifecycleNote
