import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { apiHealth } from "@/services/api";
import type { Player, Team } from "@/types";
import { playerMe } from "@/services/PlayerApi";
import { teamGet } from "@/services/TeamApi";
import { usePushNotifications } from "@/services/usePushNotifications";

const JWT_KEY = import.meta.env.VITE_JWT_KEY;

interface AuthContextValue {
    jwt: string | null;
    setJwt: (jwt: string | null) => void;
    logout: () => void;
    authLoading: boolean;
    healthy: boolean | null;
    hydrate: (token: string) => void;
    me: Player | null;
    setMe: (p: Player | null) => void;
    myTeam: Team | null;
    setMyTeam: React.Dispatch<React.SetStateAction<Team | null>>;
    refreshTeam: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const { unsubscribe, subscription } = usePushNotifications();

    const [healthy, setHealthy] = useState<boolean | null>(null);
    const [jwt, setJwt] = useState<string | null>(() => localStorage.getItem(JWT_KEY));

    const [authLoading, setAuthLoading] = useState<boolean>(!!jwt);

    const [me, setMe] = useState<Player | null>(null);
    const [myTeam, setMyTeam] = useState<Team | null>(null);

    const hydrate = (token: string) => {
        setJwt(token);
        setAuthLoading(true);
    };

    const logout = useCallback(async () => {
        // console.log("Logout started");

        if (subscription) {
            try {
                await Promise.race([
                    unsubscribe(),
                    new Promise((resolve) => setTimeout(resolve, 5000))
                ]);
                // console.log("Unsubscribed successfully");
            } catch (error) {
                // console.error("Unsubscribe failed, continuing logout:", error);
            }
        }

        // console.log("Clearing state");
        localStorage.removeItem(JWT_KEY);
        setJwt(null);
        setMe(null);
        setMyTeam(null);
        // console.log("Logout completed, redirecting...");

        window.location.href = '/capture-the-flag';
    }, [unsubscribe]);

    useEffect(() => {
        if (jwt) localStorage.setItem(JWT_KEY, jwt);
        else localStorage.removeItem(JWT_KEY);
    }, [jwt]);

    useEffect(() => {
        if (!jwt) {
            setAuthLoading(false);
            setMe(null);
            setMyTeam(null);
            return;
        }

        let cancelled = false;
        setAuthLoading(true);

        playerMe(jwt)
            .then((p) => {
                if (cancelled) return;
                setMe(p);
                teamGet(p.team, jwt).then((t) => !cancelled && setMyTeam(t)).catch(() => { });
            })
            .catch(() => {
                if (cancelled) return;
                logout();
            })
            .finally(() => {
                if (cancelled) return;
                setAuthLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [jwt, logout]);

    useEffect(() => {
        apiHealth()
            .then(() => setHealthy(true))
            .catch(() => setHealthy(false));
    }, []);

    const refreshTeam = useCallback(() => {
        if (!myTeam || !jwt) return;
        teamGet(myTeam.id, jwt).then((t) => setMyTeam(t)).catch(() => { });
    }, [myTeam, jwt])

    return (
        <AuthContext.Provider
            value={{
                jwt,
                setJwt,
                logout,
                authLoading,
                healthy,
                hydrate,
                setMe,
                me,
                myTeam,
                setMyTeam,
                refreshTeam,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuthContext = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error("useAuthContext must be used within an AuthProvider");
    return context;
};

export default AuthContext;
