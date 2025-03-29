import { create } from "zustand";

interface User {
    id?: number;
    name: string;
    email: string;
}

interface StoreState {
    users: User[];
    setUsers: (users: User[]) => void;
}

export const useStore = create<StoreState>((set) => ({
    users: [],
    setUsers: (users) => set({ users }),
}));
