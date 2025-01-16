use std::io::Write;
use std::process::Command;
use std::thread::sleep;
use std::time::Duration;
use regex::Regex;
use time::{OffsetDateTime, UtcOffset};
use utilities::next_wednesday_3am;

lazy_static::lazy_static! {
    static ref UPLINK_MODE_REGEX: Regex = Regex::new("^.+Switching to (.+) mode!!$").unwrap();
}

fn current_time() -> OffsetDateTime {
    OffsetDateTime::now_utc()
        .to_offset(UtcOffset::current_local_offset()
            .unwrap_or(UtcOffset::from_hms(5, 30, 0).unwrap()))
}

fn uplink_running() -> bool {
    Command::new("pgrep")
        .arg("-x")
        .arg("uplink")
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

fn _uplink_mode() -> Option<String> {
    let logs = format!(
        "{}\n{}",
        std::fs::read_to_string("/data/local/uplink/out.log.1").unwrap_or(String::new()),
        std::fs::read_to_string("/data/local/uplink/out.log").unwrap_or(String::new())
    );
    for line in logs.lines().rev() {
        if let Some(mode) = UPLINK_MODE_REGEX.captures(line)
            .and_then(|matches| matches.get(1)) {
            return Some(mode.as_str().to_string());
        }
    }
    return None;
}

fn _internet_working() -> bool {
    let output = Command::new("ping")
        .arg("-c")
        .arg("1")
        .arg("google.com")
        .output()
        .expect("failed to execute process");
    return output.status.success();
}

fn main() {
    // safe as long as we don't modify environment variables
    unsafe { time::util::local_offset::set_soundness(time::util::local_offset::Soundness::Unsound); }
    let now = current_time();
    let oh_boy_3am = next_wednesday_3am(now);

    let current_exe_path = std::env::current_exe().unwrap();
    let uplink_module_dir = current_exe_path
        .parent()
        .and_then(|d| d.parent())
        .unwrap_or_else(|| panic!("uplink module not installed properly"));

    let restart = || {
        let _ = Command::new(uplink_module_dir.join("bin").join("daemonize"))
            .arg(uplink_module_dir.join("service.sh"))
            .output();
    };

    println!("{now}: waiting till: {oh_boy_3am}");
    let mut _tics = 0;
    loop {
        let _ = std::io::stdout().flush();
        let _ = std::io::stderr().flush();
        sleep(Duration::from_secs(1));
        _tics += 1;
        let now = current_time();

        if now > oh_boy_3am {
            println!("{now}: restarting uplink");
            restart()
        } else if !uplink_running() {
            println!("{now}: uplink crashed, restarting");
            restart()
        }
    }
}