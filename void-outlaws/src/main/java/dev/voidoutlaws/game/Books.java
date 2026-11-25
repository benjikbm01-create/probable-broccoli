package dev.voidoutlaws.game;

import dev.voidoutlaws.reg.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Readable books (they open the normal book screen). */
public final class Books {
    private Books() {}

    static final List<String> HANDBOOK = List.of(
            "§lOUTLAW'S HANDBOOK§r\n\nWelcome to the crew of the Lucky Larceny!\n\n/guide = quick controls\n/tutorial = your current step\n/crew = crew status",
            "§lTHE SHIP§r\n\nSHIP CONSOLE (front): the star map. Click a place to JUMP there.\n\nHATCH (cargo bay): step outside.",
            "§lGETTING BACK§r\n\nEvery landing pad has a DOCKING BEACON. Right-click it to return to the ship.\n\nIf the ship left without you, the beacon still works once it docks.",
            "§lJUMPS§r\n\nA jump takes a little while. Watch the bar at the top of the screen.\n\nThings can happen on the way: scans, pirates, chases...",
            "§lPILOT§r\n\nUse the SHIP CONSOLE during an event.\n\nA star slides along a bar. Click when the star is inside the GREEN zone.\n\nThat dodges shots and wins chases.",
            "§lGUNNER§r\n\nUse the GUNNERY STATION during an event.\n\nClick the RED enemy ships (and BLUE drones) before they fire!",
            "§lMECHANIC§r\n\nUse the ENGINEERING PANEL.\n\nClick SPARKS to fix the hull. Boost shields or engines when the button is ready.\n\n(Great job for a 3rd player!)",
            "§lSMUGGLING§r\n\nTake jobs at JOB BOARDS. You get contraband to deliver.\n\nScans find contraband in your POCKETS and the CARGO HOLD.",
            "§lHIDING CARGO§r\n\nThe HIDDEN COMPARTMENT is a loose floor plate in the cargo bay.\n\nScans almost never find it (only when your Heat is very high).",
            "§lDELIVERING§r\n\nAt the destination, use its JOB BOARD or DEAD DROP.\n\nCargo is pulled straight from your pockets and the ship's holds.",
            "§lSCANS§r\n\nWhen a patrol hails you, the PILOT picks:\n\nCOMPLY - get scanned\nBRIBE - pay credits\nRUN - start a police chase!",
            "§lHEAT§r\n\nYour crew's wanted level (see the sidebar). Crimes raise it.\n\nHigh Heat = more scans, more patrols, and eventually BOUNTY HUNTERS.",
            "§lLOWER HEAT§r\n\n- Lie low at THE BURROW\n- Pay fines on Meridian Prime\n- Bribe during scans\n- Swap the ship's TRANSPONDER chip (Engineering Panel)",
            "§lLOOT§r\n\nJob pay goes into the CREW POT.\n\nSplit it at the LOOT LOCKER. Or skim a little... your crew might notice!",
            "§lBACKSTABBERS§r\n\nGrab the whole pot and run, and you'll glow for a while.\n\nYour crewmates get Space Tomatoes to throw at you. Say sorry by buying a round at a card table.",
            "§lMONEY & GEAR§r\n\nYour own credits: /crew\nGive credits: /pay\n\nBLACK MARKETS sell blasters, jetpacks, grappling hooks, disguise kits and more.",
            "§lBIG JOBS§r\n\nHeists and boarding need teamwork: one player hacks or fixes a panel, the others defend.\n\nGetting hit interrupts the hacker!",
            "§lTIPS§r\n\n- Dying sends you to the ship (small med-bay bill).\n- Stuck? /unstuck\n- Friends can join anytime.\n- Talk to people you meet!"
    );

    static final List<String> LEDGER = List.of(
            "§lLUMEN TRUST§r\n§lPRIVATE LEDGER§r\n\nAccount holder:\nK. Brightcoil\n\n§8(Very confidential. Please do not rob.)",
            "Deposit from the CONCORDAT:\n\n\"Payment on delivery of the crew of the Lucky Larceny, along with proof of every Syndicate crime, planted on their ship.\"",
            "§5Note, in purple ink:§r\n\n\"The new crew is perfect. Cheerful, loyal, and not very curious.\"",
            "§5\"Once they fetch this ledger for me, Rook collects them. The Concordat clears MY record. Everyone wins!§r\n\n§5Except them. Ha!\"",
            "Someone has doodled a frowny face in the margin.\n\nIt might have been you.\n\n§8(The Baroness wants this back. Customs Officer Pell on Meridian Prime might want it too...)"
    );

    private static WrittenBookContentComponent content(String title, String author, List<String> pages) {
        List<RawFilteredPair<Text>> ps = new ArrayList<>();
        for (String p : pages) ps.add(RawFilteredPair.of(Text.literal(p)));
        return new WrittenBookContentComponent(RawFilteredPair.of(title), author, 0, ps, true);
    }

    public static void apply(ItemStack s, boolean ledger) {
        s.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, ledger
                ? content("Lumen Ledger", "Lumen Trust", LEDGER)
                : content("Outlaw's Handbook", "The Crew", HANDBOOK));
    }

    private static ItemStack make(Item item, boolean ledger) {
        ItemStack s = new ItemStack(item);
        apply(s, ledger);
        return s;
    }

    public static ItemStack handbook() { return make(ModItems.HANDBOOK, false); }

    public static ItemStack ledger() { return make(ModItems.LUMEN_LEDGER, true); }
}
