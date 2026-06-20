/**
 * Unit tests for firestore.service.ts
 */

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
import { getGroupData, getActorDisplayName } from "../services/firestore.service";

describe("firestore.service", () => {
  let firestoreMock: ReturnType<typeof admin.firestore>;

  beforeEach(() => {
    jest.clearAllMocks();
    firestoreMock = admin.firestore();
  });

  describe("getGroupData", () => {
    it("returns group data when document exists", async () => {
      const groupData = { groupId: "g1", name: "Trip", currency: "EUR", memberIds: ["u1"] };

      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockResolvedValue({
            exists: true,
            data: () => groupData,
          }),
        }),
      });

      const result = await getGroupData("g1");
      expect(result).toEqual(groupData);
    });

    it("returns null when group document does not exist", async () => {
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockResolvedValue({
            exists: false,
          }),
        }),
      });

      const result = await getGroupData("missing");
      expect(result).toBeNull();
    });
  });

  describe("getActorDisplayName", () => {
    it("returns displayName when available", async () => {
      const userData = {
        userId: "u1",
        username: "johndoe",
        email: "john@test.com",
        displayName: "John Doe",
      };

      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockResolvedValue({
            exists: true,
            data: () => userData,
          }),
        }),
      });

      const result = await getActorDisplayName("u1");
      expect(result).toBe("John Doe");
    });

    it("falls back to username when displayName is null", async () => {
      const userData = {
        userId: "u1",
        username: "johndoe",
        email: "john@test.com",
        displayName: null,
      };

      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockResolvedValue({
            exists: true,
            data: () => userData,
          }),
        }),
      });

      const result = await getActorDisplayName("u1");
      expect(result).toBe("johndoe");
    });

    it("falls back to email when username is also empty", async () => {
      const userData = {
        userId: "u1",
        username: "",
        email: "john@test.com",
        displayName: null,
      };

      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockResolvedValue({
            exists: true,
            data: () => userData,
          }),
        }),
      });

      const result = await getActorDisplayName("u1");
      expect(result).toBe("john@test.com");
    });

    it("returns 'Someone' when user document does not exist", async () => {
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockResolvedValue({
            exists: false,
          }),
        }),
      });

      const result = await getActorDisplayName("missing");
      expect(result).toBe("Someone");
    });

    it("returns 'Someone' when Firestore throws", async () => {
      (firestoreMock.collection as jest.Mock).mockReturnValue({
        doc: jest.fn().mockReturnValue({
          get: jest.fn().mockRejectedValue(new Error("Network error")),
        }),
      });

      const result = await getActorDisplayName("u1");
      expect(result).toBe("Someone");
    });
  });
});
