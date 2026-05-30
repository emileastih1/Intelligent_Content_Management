# Ground Truth — Alex Morgan CV

Used to evaluate factual-extraction accuracy of the RAG pipeline.
Each question has an **expected answer** (what a correct response must contain)
and a **keyword set** (minimum tokens that must appear in the response to pass).

---

## Q1 — Skills (programming languages)
**Question:** What programming languages does Alex Morgan know?
**Expected:** Java, Python, SQL
**Must contain:** `Java`, `Python`, `SQL`

## Q2 — Experience (years in specific technology)
**Question:** How many years of Java experience does Alex Morgan have?
**Expected:** 6 years
**Must contain:** `6`

## Q3 — Role (most recent job title)
**Question:** What is Alex Morgan's most recent job title?
**Expected:** Senior Software Engineer
**Must contain:** `Senior Software Engineer`

## Q4 — Timeline (employer dates)
**Question:** When did Alex Morgan work at DataSoft Solutions?
**Expected:** June 2018 to March 2021
**Must contain:** `2018`, `2021`, `DataSoft`

## Q5 — Education (degree and institution)
**Question:** What degree does Alex Morgan hold and from which university?
**Expected:** MSc Computer Science from University of Edinburgh, BSc Computer Science from University of Glasgow
**Must contain:** `MSc`, `Edinburgh`, `BSc`, `Glasgow`

## Q6 — Experience (total years)
**Question:** How many years of professional experience does Alex Morgan have?
**Expected:** 6 years
**Must contain:** `6`

## Q7 — Synthesis boundary (certifications — tests cross-section retrieval)
**Question:** What AWS certifications does Alex Morgan hold?
**Expected:** AWS Certified Solutions Architect – Associate (2022)
**Must contain:** `AWS`, `Solutions Architect`, `2022`

## Q8 — Completeness probe (all employers)
**Question:** List all companies Alex Morgan has worked for.
**Expected:** TechCorp Ltd, DataSoft Solutions
**Must contain:** `TechCorp`, `DataSoft`
