# Funding Floor -- a career mode gameplay mod for KSP

The space program's funding council has deep pockets, but an equally deep aversion to bad press. They will approve any mission, sign any funding request -- as long as the cost falls within what they deem "acceptable risk". If you want to launch more expensive (and thus, in their estimation, inherently riskier) missions, you will need to prove yourself.

Funding Floor evolved out of my desire to play a career game where I never had to worry about *running out* of money, but where money and reputation still had some use and I couldn't just unlock everything from the start. Mods like Play Your Way and Daily Funds were closer to what I wanted, but not quite what I was looking for. So, once I figured out what I *was* looking for, I wrote it.

## How It Works

Your funds, rather than being a pool of money that can be emptied, are now an upper limit on how much you can spend at any one time. If your budget is $100k, you can upgrade your launchpad and fire a dozen rockets that cost $90k into the sea, and still have $100k to use -- but that $101k rocket remains out of reach.

Your budget is calculated directly from your reputation. Increase your reputation and it goes up; lose reputation and it goes back down. In this way, as your reputation increases, you can plan bigger and more ambitious missions, without ever needing to choose between launching rockets and upgrading KSC, grind contracts to afford that last comms satellite for your constellation, or fast-forward for a month to accumulate the funding for your next mission.

## Configuration

Funding Floor has a number of options, configurable via the difficulty options menu at any time.

### Min and Max Funding

Min funding is how much funding you will have at (and below) 0 reputation; you will never have less than this to spend. Max funding is how much you will have at 1000 reputation and is a hard limit on your budget. This should usually be at least 2M so that you can afford the final R&D upgrade someday; if playing with mods that add particularly expensive parts, you may want to set it higher.

### R&D Funding Bonus

For every point of science spent at the R&D center, your min funding will be permanently increased by this much. It does not affect max funding.

### Funding Penalty for Expenses

If turned on, every expense reduces your funding (by slightly reducing your reputation) by this % of the expense -- so if set to 1%, a $100k launch would reduce your budget by $1k. This gives some incentive to be frugal with your launches even once you have a large budget, but also means you need to have at least some ongoing reputation income.

### Funding Bonus for Income

This is the opposite of the funding penalty -- if you do something that makes you money (like recovering a rocket or completing a contract), you get a small reputation bonus, increasing your operating budget by this percent of the income. It is intended to provide some incentive to build reusable rockets.

### Science Bonus for Income

This is similar to the funding bonus above, except it generates science. Specifically, this percent of the income is used to purchase science points at the same $10k per 1 science pricing used by the Outsourced R&D strategy.

## Compatibility & Warnings

Funding Floor will probably misbehave if coupled with other mods that take over the economy, like KSPCasher. It should otherwise be broadly compatible.

It is highly recommended that if you turn on expense/income effects above, you do not use any strategies that convert to or from funds, such as Appreciation Campaign (funds to reputation) or Patent Licensing (science to funds). These can cause weird side effects like gaining more/less reputation than you should, contracts being counted as income and expenses at the same time, etc.
