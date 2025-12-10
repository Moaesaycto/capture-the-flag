import Container from "@/components/main/Containers";
import Page from "@/components/main/Page";
import { FaBookDead } from "react-icons/fa";
import { PiHouseBold } from "react-icons/pi";
import { Link } from "react-router-dom";

const RulesPage = () => {
    return (
        <Page isStatic>
            <div className="flex flex-col gap-4">
                <div className="flex flex-row justify-between">
                    <h2
                        className="text-4xl"
                        style={{ fontFamily: "American Captain" }}
                    >
                        Official Rules
                    </h2>
                    <Link to="/">
                        <div className="w-8 h-8 bg-amber-400 text-black rounded text-2xl flex items-center justify-center">
                            <PiHouseBold />
                        </div>
                    </Link>
                </div>
                <p>
                    In the event of an emergency, return to the rendezvous point immediately.
                </p>
                <Container title="Quick start" padding={false} Icon={FaBookDead}>
                    <div className="py-2 px-2 flex flex-col gap-4">
                        <p>
                            The objective of the game is to capture the Flag of an enemy team(s) and bring it back to your base, all while defending your Flag.
                        </p>
                        <p>
                            The game has three periods:
                        </p>
                        <ul className="list-disc pl-4 flex flex-col gap-2">
                            <li>Grace period: Safe time to plant and register team Flag/base locations.</li>
                            <li>Scouting period: Find and capture an enemy Flag while avoiding elimination.</li>
                            <li>Reveal period: All base locations are revealed.</li>
                        </ul>
                        <p>
                            You're eliminated if your tag is removed by an enemy. You must then return to your base to revive. If you eliminate a player,
                            you keep their tag. Players with the most eliminations are commended.
                        </p>
                        <p>
                            Each team is provided with twice the number of tags as there are players in the largest team. When a team runs out, they are no longer able to revive.
                        </p>
                        <p>
                            Flags and tags must always be visible. A team's base is a circular area of radius 5 metres around their Flag. Once placed, team members
                            are only able to enter the base to plant a flag or revive. You cannot eliminate anybody inside your base.
                        </p>
                        <p>
                            To win, you must have all flags safely at your base. If victory is not declared in time, the game ends and secondary win conditions will be used.
                        </p>
                        <p>
                            Physical contact is to remain at a minimum.
                        </p>
                    </div>
                </Container>
                <h2
                    className="text-3xl"
                    style={{ fontFamily: "American Captain" }}
                >
                    Objective
                </h2>
                <p>
                    To win Capture the Flag, you must declare victory with every Flag safely at your base before the time runs out. At the same time,
                    you must protect your Flag while avoiding elimination. If victory is not declared, the hierarchy of winning conditions follows:
                </p>
                <ul className="list-disc pl-4 flex flex-col gap-2">
                    <li>Number of Flags safely at a team base;</li>
                    <li>Number of enemy tags in a team's posession;</li>
                    <li>Least lives lost as a team;</li>
                </ul>
                <p>
                    If none of these win conditions are met, the game is considered a tie.
                </p>
                <hr />
                <h2
                    className="text-3xl"
                    style={{ fontFamily: "American Captain" }}
                >
                    Rules
                </h2>
                <ol className="list-decimal pl-4 flex flex-col gap-2">
                    <li>
                        The most important rule is that all players play in good faith. The game is run on an honour code and requires universal cooperation.
                        Any player who is found willingly breaking the rules or trying to subvert the rules will be asked to leave.
                    </li>
                    <li>The game structure is separated into several phases, wherein certain actions are permitted. Each phase lasts for a set amount of time,
                        at the end of which the next phase will commence, provided all game criteria have been met. The remaining time of each phase can be
                        found on the application homepage.
                    </li>
                    <li>
                        Phase 1 is the Grace Period, where teams must register the location of their base using the app.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            Players are not permitted to knowingly leave their team's territory during this phase, which will be assigned at the beginning of the game.
                        </li>
                        <li>
                            Flags must be registered on the game map in the same location they are placed in real life.
                        </li>
                        <li>
                            Failure of a team to register their Flag will result in a suspension of the game until registration is complete. Should a team
                            take an excessive amount of time to complete their registration, that team shall be disqualified by an administrator.
                        </li>
                        <li>
                            Flags must be placed and registered in a legal, accessible and fair location. See Rule 7 for Flag placement rules and etiquette.
                        </li>
                    </ol>
                    <li>
                        Phase 2 is the Scouting Period. Players are now permitted to leave their team's territory in order to search for the location of
                        other Flags and capture them.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            Players can now be eliminated. For elimination rules and regulations, see Rule 9.
                        </li>
                        <li>
                            Flags are not considered safe unless they are within your base and not held by any player (see Rule 7.X)
                        </li>
                        <li>
                            Flags are not considered captured until they are taken to a team's base. In order to declare victory, all Flags must be
                            at the declaring team's location.
                        </li>
                    </ol>
                    <li>
                        Phase 3 is the Reveal Period. This phase reveals all teams' registered base locations, and marks the final phase of the game.
                        The phase runs similarly to the Scouting Period.
                    </li>
                    <li>
                        If victory isn't declared by the end of the Reveal Period, the following win conditions are considered one at a time in order.

                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            The victor of each condition being the team with the highest number in the category of:
                        </li>
                        <ol className="list-[upper-roman] pl-8">
                            <li>Flags safely at the team's base;</li>
                            <li>Enemy tags in the team's posession;</li>
                            <li>Remaining lives for the team (including active players);</li>
                        </ol>
                        <li>
                            If there is no defined winner, then the game is considered a tie.
                        </li>
                    </ol>
                    <li>
                        The Flags are the central objects and objectives of the game of Capture the Flag.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>Each team must possess a single Flag for their team.</li>
                        <li>
                            All Flags must be of uniform size and shape, and of a vibrant colour representative of the team that registers it.
                        </li>
                        <li>An appropriate location for a Flag is defined as follows:</li>
                        <ol className="list-[upper-roman] pl-8">
                            <li>Flags must not be disguised or concealed by any player, at any point during the game (including when being captured);</li>
                            <li>Flags must not be tied off or otherwise affixed to any point or terrain feature that would render it impossible to easily
                                remove. Easy removal of a Flag constitutes picking up with a one-handed grip without needing to displace any obstacles;
                            </li>
                            <li>Flags cannot be purposefully damaged;</li>
                            <li>Flags stationed at a team's base should have at least 180 degrees of continuous open space and not be within 5 metres of a major obstacle;</li>
                        </ol>
                        <li>
                            Should a player be eliminated while holding any flag(s), they must plant or place the flag(s) down exactly where they have been eliminated.
                            The flag can be immediately recaptured.
                        </li>
                        <li>When carrying a flag, you are still permitted to eliminate enemy players.</li>
                        <li>
                            Players are not permitted to hand off their flag to another player after they have been eliminated.
                        </li>
                        <li>
                            Players are permitted to carry more than one flag at a time.
                        </li>
                        <li>
                            Your team is not permitted to relocate your flag from your base. Team members may only handle their own flag to initially plant it during the Grace
                            Period or to return it to the base after an enemy has dropped it.
                        </li>
                        <li>
                            When planting your own team's flag back at your base, the flag becomes vulnerable again as soon as it touches the ground. A waiting enemy can immediately pick it up. 
                        </li>
                        <li>
                            A flag is said to be said if it is within your base and not held by any player (friend or enemy).
                        </li>
                    </ol>
                    <li>
                        The base (or your team's registered flag location) is the area to which you must return the flags in order to win. After deciding your team's flag location,
                        your base is considered the circular area of radius 5 metres around it.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            To the best of your ability, this area should be free of any walls or similar major obstacles.
                        </li>
                        <li>
                            During the game, team members cannot enter their base area unless it is to plant a flag or to revive.
                            You may not eliminate enemy players inside this area.
                        </li>
                        <li>
                            If you are reviving, you cannot be eliminated until you have left your base.
                        </li>
                        <li>
                            If the area in which your base is located is fully enclosed and there is no reasonable way to accommodate free entry, then it is not considered an
                            appropriate location. I.e. a fully enclosed area or an area that requires jumping over a fence.
                        </li>
                        <li>
                            No part of the base can go on private property, leave the bounds of your territory or be in the water.
                        </li>
                    </ol>
                    <li>
                        Active players must be wearing tags that are tucked into the waistband or back pocket of their pants with at least
                        10cm of visible material, measured from their pants.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            Each team will be provided with tags, totalling twice the number of players on the maximum team. These will be provided at the beginning of the game.
                        </li>
                        <li>
                            You may only carry one tag of your team's colour at any point.
                        </li>
                        <li>
                            You are not permitted to wear the tag of an opposing team.
                        </li>
                        <li>
                            You are eliminated if an enemy player removes your tag.
                        </li>
                        <li>
                            If you are eliminated, you must immediately return to your base to obtain a new tag. You are only active in the game again
                            once you have affixed the new tag and left the bounds of your base.
                        </li>
                        <li>
                            If your team no longer has any available tags, you are unable to revive and you must return to the rendezvous point.
                        </li>
                        <li>
                            After being eliminated, you are not able to coordinate any actions or report information to your team members until you have revived.
                        </li>
                        <li>
                            Tags taken from enemy players can be put away and should not be visible at all. Players must retain all obtained enemy tags until the
                            end of the game (to handle secondary win conditions and for personal glory).
                        </li>
                        <li>
                            Two opposing players can eliminate each other if their tags are pulled simultaneously.
                        </li>
                        <li>
                            Eliminating players must make it obvious to the eliminated player when they have been eliminated.
                        </li>
                    </ol>
                    <li>
                        Physical contact is to remain at a minimum.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            Manoeuvring your body to avoid elimination is allowed, however you cannot physically block the tag with any part of your
                            body (i.e. with your hands or sitting on it).
                        </li>
                        <li>
                            Players cannot intentionally attack other players physically for any reason. I.e. shoving, punching, tripping, and so on.
                        </li>
                        <li>
                            Players cannot take a flag from another player's possession unwillingly. The player must first be eliminated before the flag can be recaptured.
                        </li>
                    </ol>
                    <li>
                        You declare victory on the home page of this app.
                    </li>
                    <ol className="list-[upper-roman] pl-8">
                        <li>
                            You may only declare victory if all of the following criteria are met.
                        </li>
                        <ol className="list-[upper-roman] pl-8">
                            <li>
                                All flags, including your own, are safely located in your base.
                            </li>
                            <li>
                                The game is currently in the Scouting or Reveal period.
                            </li>
                        </ol>
                        <li>
                            Until victory is declared, players are still able to recapture any flags. That is, if you fail to declare victory before another team steals it,
                            then the game continues.
                        </li>
                    </ol>
                </ol>
            </div>
        </Page>
    )
}

export default RulesPage;