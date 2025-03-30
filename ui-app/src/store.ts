import { create } from "zustand";

// Definicja typu User
interface User {
    id?: number;
    name: string;
    email: string;
}

// Definicja typu StoreState, czyli stanu aplikacji
interface StoreState {
    users: User[]; // Lista użytkowników
    setUsers: (users: User[]) => void; // Funkcja do ustawiania listy użytkowników
    addUser: (user: User) => void; // Funkcja do dodawania pojedynczego użytkownika
}

export const useStore = create<StoreState>((set) => ({
    users: [], // Początkowa lista użytkowników
    setUsers: (users) => set({ users }), // Ustawianie całej listy użytkowników
    addUser: (user) => set((state) => ({ users: [...state.users, user] })), // Dodawanie pojedynczego użytkownika
}));
