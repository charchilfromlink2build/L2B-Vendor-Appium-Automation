package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.Test;

/**
 * Free-operator UI happy path for seed {@code L2B-RNT-2026-F7D066} (site RB-FREE-OP-UI).
 * API already showed {@code all_operators_busy=false} / Nauman {@code is_available=true}.
 * This is Appium only — Accept → Assign machine → select operator → Confirm.
 * Isolated {@code quickbooking/accept-free-operator.xml}. Do not run the full accept suite.
 */
@Epic("Vendor app")
@Feature("Quick Booking accept — free operator UI (F7D066)")
public class FreeOperatorUiAcceptTest extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-free-op-ui");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final String SEED_NUMBER = "L2B-RNT-2026-F7D066";
    private static final String SEED_SITE = "RB-FREE-OP-UI";

    @Test(priority = 1, description = "Free-op UI: Accept → select operator → Confirm completes")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Seed F7D066 with a free operator (Nauman). Real taps: Accept, Assign machine, "
            + "Select an operator, Confirm. Records busy-banner vs Confirm enablement so #16 can "
            + "be scoped to busy-only or broader Confirm breakage. Do not tap Decline or Skip.")
    public void freeOperatorAcceptAssignConfirm() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        HomePage home = new HomePage();

        int before = page.acceptCount();
        String amount = page.firstAmountLine();
        String sourceBefore = DriverManager.get().getPageSource();
        boolean seedOnQueue = sourceBefore.contains(SEED_NUMBER)
                || sourceBefore.contains(SEED_SITE)
                || sourceBefore.contains("F7D066")
                || sourceBefore.contains("FREE-OP");

        Allure.parameter("phone", rentalOwnerPhone());
        Allure.parameter("seedNumber", SEED_NUMBER);
        Allure.parameter("seedSiteHint", SEED_SITE);
        Allure.parameter("seedVisibleOnQueue", String.valueOf(seedOnQueue));
        Allure.parameter("acceptCountBefore", String.valueOf(before));
        Allure.parameter("acceptedAmount", amount);
        writeDump("11-qb-before-accept", page, home);
        page.attachScreenshot("free-op-before-accept");

        assertThat(before).as("Need the free-op pending card on Quick Booking").isGreaterThanOrEqualTo(1);
        assertThat(page.isAcceptEnabled()).as("Accept outer View enabled").isTrue();
        assertThat(page.isExtendTimeDialogVisible()).as("Must not be extend-time dialog").isFalse();

        page.tapFirstAccept();
        Waits.until(DriverManager.get(),
                d -> page.isAssignMachineVisible() ? Boolean.TRUE : null,
                "After Accept, Assign machine did not open",
                Duration.ofSeconds(15));

        String assignSource = DriverManager.get().getPageSource();
        boolean busyBanner = assignSource.contains("All operators are busy")
                || assignSource.contains("operators are busy for this slot");
        boolean confirmBeforePick = page.isAssignConfirmEnabled();
        Allure.parameter("assignMachine", "true");
        Allure.parameter("selectOperator", String.valueOf(page.isSelectOperatorVisible()));
        Allure.parameter("busyBannerOnAssign", String.valueOf(busyBanner));
        Allure.parameter("confirmEnabledBeforeOperatorPick", String.valueOf(confirmBeforePick));
        Allure.parameter("assignVisibleTexts", String.join(" | ", visibleTexts(assignSource)));
        writeDump("12-assign-machine", page, home);
        page.attachScreenshot("free-op-assign-machine");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isAssignMachineVisible()).as("Accept opens Assign machine").isTrue();
        assertThat(page.isSelectOperatorVisible()).as("Assign has Select an operator").isTrue();
        assertThat(busyBanner)
                .as("Free-op seed must not show the busy-operator banner (that path is #16)")
                .isFalse();

        String operator = page.selectFirstOperator();
        boolean operatorRowEnabled = page.lastMenuOptionEnabled();
        boolean confirmAfterPick = page.isAssignConfirmEnabled();
        String afterPickSource = DriverManager.get().getPageSource();
        boolean busyAfterPick = afterPickSource.contains("All operators are busy")
                || afterPickSource.contains("operators are busy for this slot");

        Allure.parameter("assignedOperator", operator);
        Allure.parameter("operatorRowEnabled", String.valueOf(operatorRowEnabled));
        Allure.parameter("confirmEnabledAfterAssign", String.valueOf(confirmAfterPick));
        Allure.parameter("busyBannerAfterOperatorPick", String.valueOf(busyAfterPick));
        writeDump("13-operator-selected", page, home);
        page.attachScreenshot("free-op-operator-selected");

        assertThat(operator).as("An operator was chosen from the dropdown").isNotBlank();
        assertThat(operatorRowEnabled)
                .as("Free operator row must be enabled (not greyed like #16 busy path)")
                .isTrue();
        assertThat(confirmAfterPick)
                .as("Confirm must enable after picking a free operator — if false, #16 is broader "
                        + "than busy-only")
                .isTrue();

        page.tapAssignConfirm();
        Waits.until(DriverManager.get(),
                d -> (!page.isAssignMachineVisible()
                        && (page.isDisplayedNow() || home.isDisplayedNow())) ? Boolean.TRUE : null,
                "After free-operator Confirm, Assign machine stayed open — Accept did not complete",
                Duration.ofSeconds(20));

        int after = page.acceptCount();
        boolean stillQb = page.isDisplayedNow();
        boolean homeNow = home.isDisplayedNow();
        Allure.parameter("assignMachineAfterConfirm", String.valueOf(page.isAssignMachineVisible()));
        Allure.parameter("acceptCountAfterConfirm", String.valueOf(after));
        Allure.parameter("stillOnQuickBooking", String.valueOf(stillQb));
        Allure.parameter("homeVisible", String.valueOf(homeNow));
        Allure.parameter("acceptCompleted", "true");
        writeDump("14-after-confirm", page, home);
        page.attachScreenshot("free-op-after-confirm");

        assertThat(page.isAssignMachineVisible()).as("Assign machine must close after Confirm").isFalse();
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
        assertThat(after < before || homeNow)
                .as("Accept must finish (card left queue or Home)")
                .isTrue();
        if (stillQb) {
            assertThat(after).as("Accepted free-op card left the queue").isLessThan(before);
        }
    }

    private static Set<String> visibleTexts(String xml) {
        Matcher m = TEXT.matcher(xml == null ? "" : xml);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String t = m.group(1);
            if (!t.startsWith("http") && !t.contains("com.android")) {
                out.add(t);
            }
        }
        return out;
    }

    private void writeDump(String tag, QuickBookingLandingPage page, HomePage home) {
        String xml = DriverManager.get().getPageSource();
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "seed=" + SEED_NUMBER
                            + "\npackage=" + vendorPackage()
                            + "\nassignMachine=" + page.isAssignMachineVisible()
                            + "\nselectOperator=" + page.isSelectOperatorVisible()
                            + "\nconfirmEnabled=" + page.isAssignConfirmEnabled()
                            + "\nquickBooking=" + page.isDisplayedNow()
                            + "\nhome=" + home.isDisplayedNow()
                            + "\nacceptCount=" + page.acceptCount()
                            + "\nbusyBanner=" + (xml.contains("operators are busy"))
                            + "\ntexts=\n" + String.join("\n", visibleTexts(xml)) + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write free-op dump " + tag, e);
        }
    }
}
