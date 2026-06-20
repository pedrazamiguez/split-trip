/**
 * Unit tests for token.service.ts
 *
 * Mocks Firestore to test:
 * - Group member resolution
 * - Actor exclusion
 * - Multi-device token collection
 * - Edge cases (empty members, no devices)
 */

// Must mock firebase-admin BEFORE importing the service
jest.mock("firebase-admin", () => {
  const firestoreMock = {
    collection: jest.fn(),
  };

  return {
    firestore: jest.fn(() => firestoreMock),
    initializeApp: jest.fn(),
  };
});

import * as admin from "firebase-admin";
import {
  getRecipientTokens,
  getGroupMemberUserIds,
  getUserDeviceTokens,
} from "../services/token.service";

// Helper to create a mock Firestore query chain
function mockFirestoreChain(docs: Array<{ data: () => Record<string, unknown> }>) {
  const getMock = jest.fn().mockResolvedValue({ docs });
  const collectionMock = jest.fn().mockReturnValue({ get: getMock });
  const docMock = jest.fn().mockReturnValue({ collection: collectionMock });
  return { collectionMock, docMock, getMock };
}

describe("token.service", () => {
  let firestoreMock: ReturnType<typeof admin.firestore>;

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = admin.firestore();
  });

  describe("getGroupMemberUserIds", () => {
    it("returns user IDs from group members subcollection", async () => {
      const memberDocs = [
        { data: () => ({ userId: "user1", role: "ADMIN" }) },
        { data: () => ({ userId: "user2", role: "MEMBER" }) },
        { data: () => ({ userId: "user3", role: "MEMBER" }) },
      ];

      const chain = mockFirestoreChain(memberDocs);
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: chain.docMock,
      });
      chain.docMock.mockReturnValue({ collection: chain.collectionMock });
      chain.collectionMock.mockReturnValue({ get: chain.getMock });

      const result = await getGroupMemberUserIds("groupA");
      expect(result).toEqual(["user1", "user2", "user3"]);
    });

    it("filters out members with empty userId", async () => {
      const memberDocs = [
        { data: () => ({ userId: "user1" }) },
        { data: () => ({ userId: "" }) },
        { data: () => ({ userId: null }) },
      ];

      const chain = mockFirestoreChain(memberDocs);
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: chain.docMock,
      });
      chain.docMock.mockReturnValue({ collection: chain.collectionMock });
      chain.collectionMock.mockReturnValue({ get: chain.getMock });

      const result = await getGroupMemberUserIds("groupA");
      expect(result).toEqual(["user1"]);
    });
  });

  describe("getUserDeviceTokens", () => {
    it("returns tokens from user devices subcollection", async () => {
      const deviceDocs = [
        { data: () => ({ token: "token_a", model: "Pixel" }) },
        { data: () => ({ token: "token_b", model: "Samsung" }) },
      ];

      const chain = mockFirestoreChain(deviceDocs);
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: chain.docMock,
      });
      chain.docMock.mockReturnValue({ collection: chain.collectionMock });
      chain.collectionMock.mockReturnValue({ get: chain.getMock });

      const result = await getUserDeviceTokens("user1");
      expect(result).toEqual(["token_a", "token_b"]);
    });

    it("filters out empty tokens", async () => {
      const deviceDocs = [
        { data: () => ({ token: "valid_token" }) },
        { data: () => ({ token: "" }) },
      ];

      const chain = mockFirestoreChain(deviceDocs);
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: chain.docMock,
      });
      chain.docMock.mockReturnValue({ collection: chain.collectionMock });
      chain.collectionMock.mockReturnValue({ get: chain.getMock });

      const result = await getUserDeviceTokens("user1");
      expect(result).toEqual(["valid_token"]);
    });
  });

  describe("getRecipientTokens", () => {
    it("excludes the actor and collects tokens from remaining members", async () => {
      // Set up the members call chain
      const memberDocs = [
        { data: () => ({ userId: "actor" }) },
        { data: () => ({ userId: "user2" }) },
        { data: () => ({ userId: "user3" }) },
      ];

      const deviceDocsUser2 = [{ data: () => ({ token: "token_user2_device1" }) }];

      const deviceDocsUser3 = [
        { data: () => ({ token: "token_user3_device1" }) },
        { data: () => ({ token: "token_user3_device2" }) },
      ];

      // We need to mock a complex Firestore chain for multiple calls
      (firestoreMock.collection as jest.Mock).mockImplementation((path: string) => {
        if (path === "groups") {
          return {
            doc: (_groupId: string) => ({
              collection: (sub: string) => {
                if (sub === "members") {
                  return { get: jest.fn().mockResolvedValue({ docs: memberDocs }) };
                }
                return { get: jest.fn().mockResolvedValue({ docs: [] }) };
              },
            }),
          };
        }
        if (path === "users") {
          return {
            doc: (userId: string) => ({
              collection: (_sub: string) => {
                if (userId === "user2") {
                  return { get: jest.fn().mockResolvedValue({ docs: deviceDocsUser2 }) };
                }
                if (userId === "user3") {
                  return { get: jest.fn().mockResolvedValue({ docs: deviceDocsUser3 }) };
                }
                return { get: jest.fn().mockResolvedValue({ docs: [] }) };
              },
            }),
          };
        }
        return { doc: jest.fn() };
      });

      const tokens = await getRecipientTokens("groupA", "actor");
      expect(tokens).toEqual(["token_user2_device1", "token_user3_device1", "token_user3_device2"]);
    });

    it("returns empty array when all members are the actor", async () => {
      const memberDocs = [{ data: () => ({ userId: "actor" }) }];

      (firestoreMock.collection as jest.Mock).mockImplementation(() => ({
        doc: () => ({
          collection: () => ({
            get: jest.fn().mockResolvedValue({ docs: memberDocs }),
          }),
        }),
      }));

      const tokens = await getRecipientTokens("groupA", "actor");
      expect(tokens).toEqual([]);
    });
  });
});
