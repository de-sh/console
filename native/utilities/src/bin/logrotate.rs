use clap::{arg, Parser};
use time::{OffsetDateTime, UtcOffset};
use utilities::LazyFile;

#[derive(Parser, Debug)]
struct Args {
    #[arg(long)]
    output: String,
    #[arg(long, default_value_t = 10 * 1024 * 1024)]
    max_size: u64,
    #[arg(long, default_value_t = 5)]
    backup_files_count: u64,
}

fn end(msg: &str) -> ! {
    eprintln!("{}", msg);
    std::process::exit(1);
}

fn main() {
    let args: Args = Args::parse();
    let output_file = LazyFile::new(args.output.to_string());

    if output_file.exists() && !output_file.is_file() {
        end(format!("{output_file} is not a regular file").as_str());
    }
    for line in std::io::stdin().lines() {
        if !output_file.exists() {
            output_file.create();
            output_file.append_line(
                OffsetDateTime::now_utc().to_offset(UtcOffset::from_hms(5, 30, 0).unwrap())
                    .to_string().as_str()
            );
        }
        if line.is_err() {
            break;
        }
        let line = line.unwrap();

        output_file.append_line(line.as_str());
        if output_file.size() > args.max_size {
            let last_file = LazyFile::new(if args.backup_files_count == 0 {
                args.output.to_string()
            } else {
                format!("{}.{}", args.output, args.backup_files_count)
            });
            if last_file.exists() {
                last_file.delete();
            }
            for idx in (1..=args.backup_files_count).rev() {
                let old_file = LazyFile::new(
                    if idx == 1 {
                        args.output.to_string()
                    } else {
                        format!("{}.{}", args.output, idx-1)
                    }
                );
                let new_file = LazyFile::new(format!("{}.{}", args.output, idx));
                if old_file.exists() {
                    std::fs::rename(old_file.name(), new_file.name()).unwrap();
                }
            }
        }
    }
}
