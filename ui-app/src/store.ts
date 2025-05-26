import { create } from "zustand";

// Definicja typu User
interface User {
    id?: number;
    name: string;
    email: string;
}

interface StoreState {
    users: User[];
    isAuthenticated: boolean;
    userRole: string | null;
    setUsers: (users: User[]) => void;
    addUser: (user: User) => void;
    setAuthenticated: (status: boolean) => void;
    setUserRole: (role: string | null) => void;
    logout: () => void;
}

export const useStore = create<StoreState>((set) => ({
    users: [],
    isAuthenticated: !!localStorage.getItem("userId"),
    userRole: localStorage.getItem("userRole"),
    setUsers: (users) => set({ users }),
    addUser: (user) => set((state) => ({ users: [...state.users, user] })),
    setAuthenticated: (status) => set({ isAuthenticated: status }),
    setUserRole: (role) => {
        localStorage.setItem("userRole", role || "");
        set({ userRole: role });
    },
    logout: () => {
        localStorage.removeItem("userId");
        localStorage.removeItem("userRole");
        set({ 
            isAuthenticated: false,
            users: [],
            userRole: null
        });
    },
}));
