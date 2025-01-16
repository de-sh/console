use std::fmt::{Debug, Display, Formatter};
use std::io::Write;
use std::ops::Add;
use std::time::Duration;
use time::{OffsetDateTime, UtcOffset};

#[derive(Debug)]
pub struct LazyFile {
    path: String
}

impl LazyFile {
    pub fn new(path: String) -> LazyFile {
        LazyFile { path }
    }

    pub fn exists(&self) -> bool {
        std::path::Path::new(self.path.as_str()).exists()
    }

    pub fn is_file(&self) -> bool {
        std::path::Path::new(self.path.as_str()).is_file()
    }

    pub fn size(&self) -> u64 {
        std::fs::metadata(self.path.as_str()).unwrap().len()
    }

    pub fn name(&self) -> &str {
        self.path.as_str()
    }

    pub fn create(&self) {
            std::fs::File::create(self.path.as_str()).unwrap();
    }

    pub fn delete(&self) {
        if self.exists() {
            std::fs::remove_file(self.path.as_str()).unwrap();
        }
    }

    pub fn append_line(&self, line: &str) {
        if !self.exists() {
            self.create();
        }
        let mut fd = std::fs::OpenOptions::new().append(true).open(self.path.as_str()).unwrap();
        fd.write(line.as_bytes()).unwrap();
        fd.write(&['\n' as u8]).unwrap();
    }
}

impl Display for LazyFile {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.path)
    }
}

pub trait Trace<T> {
    fn trace(self) -> T;
}

impl<T: Debug> Trace<T> for T {
    fn trace(self) -> T {
        println!("{:?}", self);
        self
    }
}

pub fn next_wednesday_3am(anchor: OffsetDateTime) -> OffsetDateTime {
    let current_weekday = anchor.weekday().number_days_from_sunday();
    let wednesday_weekday = time::Weekday::Wednesday.number_days_from_sunday();
    let three_am = time::Time::from_hms(3, 30, 0).unwrap();
    let before_wednesday_case = current_weekday < wednesday_weekday || (current_weekday == wednesday_weekday && anchor.time() < three_am);
    // let after_wednesday_case = current_weekday > wednesday_weekday || (current_weekday == wednesday_weekday && anchor.time() >= three_am);
    if before_wednesday_case {
        anchor.add(Duration::from_secs(60 * 60 * 24 * ((wednesday_weekday - current_weekday) as u64)))
            .replace_time(three_am)
    } else {
        anchor.add(Duration::from_secs(60 * 60 * 24 * ((7 - (current_weekday - wednesday_weekday)) as u64)))
            .replace_time(three_am)
    }
}

lazy_static::lazy_static! {
    pub static ref INDIA_OFFSET: UtcOffset = UtcOffset::from_hms(5, 30, 0).ok().unwrap();
}

#[cfg(test)]
mod test {
    use time::{Date, Month, OffsetDateTime, PrimitiveDateTime, Time, UtcOffset};
    use crate::{INDIA_OFFSET, next_wednesday_3am};
    
    #[test]
    fn t2() {
        dbg!(OffsetDateTime::now_utc().to_offset(Utc))
    }

    #[test]
    fn t1() {
        assert_eq!(
            next_wednesday_3am(
                PrimitiveDateTime::new(
                    Date::from_calendar_date(2023, Month::April, 14).unwrap(),
                    Time::from_hms(13, 20, 0).unwrap()
                ).assume_offset(INDIA_OFFSET)
            ),
            PrimitiveDateTime::new(
                Date::from_calendar_date(2023, Month::April, 19).unwrap(),
                Time::from_hms(3, 30, 0).unwrap()
            ).assume_offset(INDIA_OFFSET)
        );

        assert_eq!(
            next_wednesday_3am(
                PrimitiveDateTime::new(
                    Date::from_calendar_date(2023, Month::April, 18).unwrap(),
                    Time::from_hms(13, 20, 0).unwrap()
                ).assume_offset(INDIA_OFFSET)
            ),
            PrimitiveDateTime::new(
                Date::from_calendar_date(2023, Month::April, 19).unwrap(),
                Time::from_hms(3, 30, 0).unwrap()
            ).assume_offset(INDIA_OFFSET)
        );

        assert_eq!(
            next_wednesday_3am(
                PrimitiveDateTime::new(
                    Date::from_calendar_date(2023, Month::April, 19).unwrap(),
                    Time::from_hms(1, 20, 0).unwrap()
                ).assume_offset(INDIA_OFFSET)
            ),
            PrimitiveDateTime::new(
                Date::from_calendar_date(2023, Month::April, 19).unwrap(),
                Time::from_hms(3, 30, 0).unwrap()
            ).assume_offset(INDIA_OFFSET)
        );

        assert_eq!(
            next_wednesday_3am(
                PrimitiveDateTime::new(
                    Date::from_calendar_date(2023, Month::April, 19).unwrap(),
                    Time::from_hms(13, 20, 0).unwrap()
                ).assume_offset(INDIA_OFFSET)
            ),
            PrimitiveDateTime::new(
                Date::from_calendar_date(2023, Month::April, 26).unwrap(),
                Time::from_hms(3, 30, 0).unwrap()
            ).assume_offset(INDIA_OFFSET)
        );
    }
}
