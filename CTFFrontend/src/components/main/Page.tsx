import type { ReactNode } from "react"
import { PiWifiXBold } from "react-icons/pi"
import Spinner from "./LoadingSpinner"
import { useAuthContext } from "../contexts/AuthContext";
import { useGameContext } from "../contexts/GameContext";
import clsx from "clsx";

type PageProps = {
    children?: ReactNode;
    padding?: boolean;
    isStatic? : boolean;
}

const Page = ({ children, padding = true, isStatic = false }: PageProps) => {
    const { authLoading, healthy } = useAuthContext();
    const { loading, health } = useGameContext();

    if (!isStatic && (authLoading || (loading && !health))) return <Loading />;
    if (!isStatic && !healthy) return <NoConnection />;

    return (
        <div
            className={clsx(
                "flex flex-col flex-1 w-full min-h-0 text-white",
                padding && "py-5 px-5"
            )}
        >
            {children}
        </div>
    );
}

const Loading = () => (
    <div className="flex-1 flex flex-col items-center justify-center bg-neutral-800">
        <div className="text-white">
            <Spinner size={100} />
        </div>
    </div>
)

const NoConnection = () => (
    <div className="flex-1 flex flex-col items-center justify-center bg-neutral-800 text-white">
        <PiWifiXBold size={64} className="text-red-500 mb-4" />
        <p className="text-lg">No Connection to Server</p>
        <p className="opacity-50">
            It seems the game has not started yet
        </p>
    </div>
)

export default Page
